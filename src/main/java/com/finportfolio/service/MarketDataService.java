package com.finportfolio.service;

import com.finportfolio.dto.PriceResponse;
import com.finportfolio.service.external.BinanceClient;
import com.finportfolio.service.external.ExchangeRateClient;
import com.finportfolio.service.external.GoldClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tum piyasa fiyatlarini yoneten merkezi servis.
 * Cache: 30 saniye in-memory
 *
 * Veri akisi:
 *   1. USD/TRY kurunu cek (her sey buna bagli)
 *   2. Kripto fiyatlarini Binance'den cek (USDT cinsinden, USDT~USD)
 *   3. Doviz fiyatlarini ExchangeRate'den cek
 *   4. Altin/gumus ons fiyatlarini cek, gram TL hesapla
 *   5. Hepsini TL cinsinden cache'le
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private static final long CACHE_TTL_MS = 30_000; // 30 saniye

    public static final String[] SYMBOLS = {
            "BTC", "ETH", "BNB", "SOL", "XRP", "ADA",
            "GRAM_ALTIN", "CEYREK_ALTIN", "YARIM_ALTIN", "TAM_ALTIN",
            "GRAM_GUMUS",
            "USD", "EUR", "GBP"
    };

    private final BinanceClient binanceClient;
    private final ExchangeRateClient exchangeRateClient;
    private final GoldClient goldClient;

    // In-memory cache
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Bir sembolun TL fiyatini doner.
     * Once cache'e bakar, yoksa tum fiyatlari topluca yeniler.
     */
    public BigDecimal getPriceTry(String symbol) {
        CacheEntry entry = cache.get(symbol);
        if (entry != null && !entry.isExpired()) {
            return entry.price;
        }

        // Cache miss veya expired - tum fiyatlari yenile
        refreshAllPrices();

        CacheEntry refreshed = cache.get(symbol);
        return refreshed != null ? refreshed.price : null;
    }

    /**
     * Tum desteklenen enstrumanlarin guncel TL fiyatlarini doner.
     */
    public List<PriceResponse> getAllPrices() {
        List<PriceResponse> result = new ArrayList<>();
        for (String symbol : SYMBOLS) {
            BigDecimal price = getPriceTry(symbol);
            if (price != null) {
                result.add(new PriceResponse(symbol, price));
            }
        }
        return result;
    }

    /**
     * Tum fiyatlari dis API'lerden cekip cache'e yazar.
     * Senkronize cunku ayni anda birden fazla cagri olabilir.
     */
    private synchronized void refreshAllPrices() {
        // Eger baska bir thread cache'i yeniden doldurmussa bir kez daha kontrol
        CacheEntry sample = cache.get("USD");
        if (sample != null && !sample.isExpired()) {
            return;
        }

        log.info("Piyasa fiyatlari yenileniyor...");
        long start = System.currentTimeMillis();

        Map<String, BigDecimal> newPrices = new HashMap<>();

        // 1. Doviz kurlarini cek
        Map<String, BigDecimal> usdRates = exchangeRateClient.getAllRatesFromUsd();
        BigDecimal usdTryRate = usdRates.get("TRY");

        if (usdTryRate == null) {
            log.error("USD/TRY kuru alinamadi - tum fiyatlar etkilenecek");
            return;
        }

        // 2. Doviz fiyatlarini TL olarak ekle
        newPrices.put("USD", usdTryRate);

        BigDecimal eurTry = exchangeRateClient.getRateToTry("EUR");
        if (eurTry != null) newPrices.put("EUR", eurTry);

        BigDecimal gbpTry = exchangeRateClient.getRateToTry("GBP");
        if (gbpTry != null) newPrices.put("GBP", gbpTry);

        // 3. Kripto fiyatlarini cek - CoinGecko zaten TL olarak donuyor, cevirme gerek yok
        Map<String, BigDecimal> cryptoTry = binanceClient.getAllPricesUsdt();
        newPrices.putAll(cryptoTry);

        // 4. Altin ve gumus
        BigDecimal gramAltin = goldClient.getGramAltinTry(usdTryRate);
        if (gramAltin != null) {
            newPrices.put("GRAM_ALTIN", gramAltin);
            newPrices.put("CEYREK_ALTIN", goldClient.getCeyrekAltinTry(gramAltin));
            newPrices.put("YARIM_ALTIN", goldClient.getYarimAltinTry(gramAltin));
            newPrices.put("TAM_ALTIN", goldClient.getTamAltinTry(gramAltin));
        }

        BigDecimal gramGumus = goldClient.getGramGumusTry(usdTryRate);
        if (gramGumus != null) {
            newPrices.put("GRAM_GUMUS", gramGumus);
        }

        // 5. Cache'i guncelle
        long now = System.currentTimeMillis();
        long expiresAt = now + CACHE_TTL_MS;
        for (Map.Entry<String, BigDecimal> entry : newPrices.entrySet()) {
            cache.put(entry.getKey(), new CacheEntry(entry.getValue(), expiresAt));
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Piyasa fiyatlari yenilendi ({} sembol, {} ms)", newPrices.size(), elapsed);
    }

    private record CacheEntry(BigDecimal price, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}