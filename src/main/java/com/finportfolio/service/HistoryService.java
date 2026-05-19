package com.finportfolio.service;

import com.finportfolio.dto.CandleResponse;
import com.finportfolio.service.external.BinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Tarihsel fiyat (OHLCV) verisi servisi.
 *
 * Kripto -> Binance gercek veri
 * Doviz/Altin -> Guncel fiyat + son 30 gun icin sentetik yumusatma
 *   (Ucretsiz tarihsel altin/doviz API'leri kararsiz oldugu icin)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private static final Set<String> CRYPTO_SYMBOLS = Set.of("BTC", "ETH", "BNB", "SOL", "XRP", "ADA");

    private final BinanceClient binanceClient;
    private final MarketDataService marketDataService;

    /**
     * Bir sembolun tarihsel mum verisini doner.
     * Kripto icin Binance'den, diger icin sentetik veri.
     */
    public List<CandleResponse> getHistory(String symbol, String interval, int limit) {
        symbol = symbol.toUpperCase();

        if (CRYPTO_SYMBOLS.contains(symbol)) {
            return binanceClient.getKlines(symbol, interval, limit);
        }

        // Doviz, altin, gumus icin sentetik veri
        return generateSyntheticHistory(symbol, interval, limit);
    }

    /**
     * Guncel fiyat etrafinda hafif rastgele dalgalanmali mum verisi uretir.
     * Demo amacli - gercek tarihsel veri yok ama grafik bos kalmasin diye.
     */
    private List<CandleResponse> generateSyntheticHistory(String symbol, String interval, int limit) {
        BigDecimal currentPrice = marketDataService.getPriceTry(symbol);
        if (currentPrice == null) {
            log.warn("Sentetik veri uretilemiyor - guncel fiyat alinamadi: {}", symbol);
            return List.of();
        }

        long intervalSeconds = parseIntervalToSeconds(interval);
        long now = Instant.now().getEpochSecond();
        // Mumlari interval'a hizala
        long alignedNow = (now / intervalSeconds) * intervalSeconds;

        // Seed'i sembolden uret ki ayni cagrida ayni veri donsun
        Random random = new Random(symbol.hashCode());

        List<CandleResponse> result = new ArrayList<>();
        BigDecimal price = currentPrice;

        // Geriye dogru git, sonra ters cevir
        List<CandleResponse> reversed = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            long time = alignedNow - (long) i * intervalSeconds;

            // %0.5'lik rastgele dalgalanma
            double changePercent = (random.nextGaussian() * 0.005);
            BigDecimal priceChange = price.multiply(BigDecimal.valueOf(changePercent));
            BigDecimal previousPrice = price.subtract(priceChange).setScale(4, RoundingMode.HALF_UP);

            BigDecimal open = previousPrice;
            BigDecimal close = price;
            BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1 + Math.abs(random.nextGaussian() * 0.002)))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(1 - Math.abs(random.nextGaussian() * 0.002)))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal volume = BigDecimal.valueOf(1000 + random.nextDouble() * 5000).setScale(2, RoundingMode.HALF_UP);

            reversed.add(new CandleResponse(time, open, high, low, close, volume));
            price = previousPrice;
        }

        // En eski mum once gelmeli (TradingView'in beklentisi)
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }

        return result;
    }

    private long parseIntervalToSeconds(String interval) {
        return switch (interval) {
            case "1m" -> 60;
            case "5m" -> 300;
            case "15m" -> 900;
            case "1h" -> 3600;
            case "4h" -> 14400;
            case "1d" -> 86400;
            case "1w" -> 604800;
            default -> 3600; // varsayilan 1 saat
        };
    }
    
    
    /**
     * Belirli bir tarihteki fiyatı doner.
     * Kripto icin Binance'den gercek veri (gunluk mum)
     * Doviz/altin icin sentetik (mevcut fiyat * tarihsel volatilite faktoru)
     */
    public java.math.BigDecimal getHistoricalPrice(String symbol, java.time.LocalDate date) {
        symbol = symbol.toUpperCase();

        if (CRYPTO_SYMBOLS.contains(symbol)) {
            return getCryptoPriceOnDate(symbol, date);
        }

        return getSyntheticPriceOnDate(symbol, date);
    }

    /**
     * Bitcoin'in 2025-01-01'deki fiyatini bulmak gibi.
     * Binance'de gunluk mum cekip o gune denk gelen close fiyatini doneriz.
     */
    private java.math.BigDecimal getCryptoPriceOnDate(String symbol, java.time.LocalDate date) {
        // Bugun ile istenen tarih arasi kac gun
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(date, java.time.LocalDate.now());
        if (daysBetween < 0) return null;
        if (daysBetween > 1000) daysBetween = 1000; // Binance max limit

        int limit = (int) (daysBetween + 5);
        List<CandleResponse> candles = binanceClient.getKlines(symbol, "1d", limit);

        if (candles.isEmpty()) return null;

        // En yakin gunu bul
        long targetEpoch = date.atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond();
        CandleResponse closest = null;
        long smallestDiff = Long.MAX_VALUE;

        for (CandleResponse c : candles) {
            long diff = Math.abs(c.time() - targetEpoch);
            if (diff < smallestDiff) {
                smallestDiff = diff;
                closest = c;
            }
        }

        if (closest == null) return null;

        // Kripto fiyati USDT cinsinden, TL'ye cevirmek lazim
        // Basit yaklasim: USDT * mevcut USD/TRY kuru (tarihsel TRY kuru karmaşık)
        java.math.BigDecimal usdtPrice = closest.close();
        java.math.BigDecimal usdTry = marketDataService.getPriceTry("USD");
        if (usdTry == null) return null;

        return usdtPrice.multiply(usdTry);
    }

    /**
     * Doviz/altin icin: bugunku fiyatin etrafinda deterministik dalgalanma.
     * Gercek tarihsel veri yok, ama "1 ay once daha ucuzdu" gibi mantikli sonuc verir.
     * Seed sembol + tarih, ayni cagrida ayni sonuc.
     */
    private java.math.BigDecimal getSyntheticPriceOnDate(String symbol, java.time.LocalDate date) {
        java.math.BigDecimal currentPrice = marketDataService.getPriceTry(symbol);
        if (currentPrice == null) return null;

        long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(date, java.time.LocalDate.now());
        if (daysAgo < 0) return null;
        if (daysAgo == 0) return currentPrice;

        // Yillik bazda %20-40 enflasyon varsayimi - dolar/altin uzun vadede degerlenir
        // Yani gecmiste fiyat daha dusuktu
        double annualGrowth = switch (symbol) {
            case "USD", "EUR", "GBP" -> 0.30;        // Doviz: %30 yillik artis (TL'ye karsi)
            case "GRAM_ALTIN", "CEYREK_ALTIN", "YARIM_ALTIN", "TAM_ALTIN" -> 0.40; // Altin daha hizli
            case "GRAM_GUMUS" -> 0.35;
            default -> 0.25;
        };

        double yearsAgo = daysAgo / 365.0;
        // Gecmis fiyat = bugunku / (1 + buyume)^yil
        double pastFactor = Math.pow(1 + annualGrowth, yearsAgo);

        // Kucuk bir rastgele dalgalanma ekle (deterministik seed)
        java.util.Random random = new java.util.Random((long) (symbol.hashCode() + date.toEpochDay()));
        double noise = 1 + (random.nextGaussian() * 0.05); // ±%5

        return currentPrice
                .divide(java.math.BigDecimal.valueOf(pastFactor * noise), 4, java.math.RoundingMode.HALF_UP);
    }
}