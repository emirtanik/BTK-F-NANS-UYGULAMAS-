package com.finportfolio.service;

import com.finportfolio.dto.CandleResponse;
import com.finportfolio.dto.TechnicalIndicatorsResponse;
import com.finportfolio.service.external.BinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Teknik analiz servisi - kripto fiyat verilerinden RSI, SMA, momentum,
 * volatilite gibi gostergeleri hesaplar ve Turkce yorum uretir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalAnalysisService {

    private final BinanceClient binanceClient;
    private final MarketDataService marketDataService;

    public TechnicalIndicatorsResponse analyze(String symbol) {
        symbol = symbol.toUpperCase();

        BigDecimal currentPrice = marketDataService.getPriceTry(symbol);
        if (currentPrice == null) {
            return emptyResponse(symbol, "Fiyat bilgisi alinamadi");
        }

        List<CandleResponse> candles = binanceClient.getKlines(symbol, "1d", 60);

        if (candles == null || candles.size() < 14) {
            return new TechnicalIndicatorsResponse(
                    symbol, currentPrice,
                    null, null, "VERI_YOK",
                    null, "VERI_YOK",
                    null, null, "VERI_YOK",
                    null, "VERI_YOK",
                    "YATAY",
                    "Bu varlik icin yeterli tarihsel veri yok. Teknik analiz yapilamiyor."
            );
        }

        BigDecimal[] closes = candles.stream()
                .map(CandleResponse::close)
                .toArray(BigDecimal[]::new);

        BigDecimal sma20 = calculateSMA(closes, 20);
        BigDecimal sma50 = closes.length >= 50 ? calculateSMA(closes, 50) : null;
        String maSignal = buildMaSignal(currentPrice, sma20, sma50);

        BigDecimal rsi14 = calculateRSI(closes, 14);
        String rsiSignal = buildRsiSignal(rsi14);

        BigDecimal change7d = calculateChange(closes, 7);
        BigDecimal change30d = calculateChange(closes, 30);
        String momentumSignal = buildMomentumSignal(change7d, change30d);

        BigDecimal volatility = calculateVolatility(closes, 20);
        String volatilityLevel = buildVolatilityLevel(volatility);

        String overallTrend = buildOverallTrend(currentPrice, sma20, sma50, rsi14, change30d);

        String recommendation = buildRecommendation(symbol, currentPrice, sma20, sma50,
                rsi14, rsiSignal, change7d, change30d, momentumSignal, volatility,
                volatilityLevel, overallTrend);

        return new TechnicalIndicatorsResponse(
                symbol, currentPrice,
                sma20, sma50, maSignal,
                rsi14, rsiSignal,
                change7d, change30d, momentumSignal,
                volatility, volatilityLevel,
                overallTrend, recommendation
        );
    }

    private BigDecimal calculateSMA(BigDecimal[] prices, int period) {
        if (prices.length < period) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = prices.length - period; i < prices.length; i++) {
            sum = sum.add(prices[i]);
        }
        return sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRSI(BigDecimal[] prices, int period) {
        if (prices.length < period + 1) return BigDecimal.valueOf(50);

        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;

        int start = prices.length - period - 1;
        for (int i = start + 1; i < prices.length; i++) {
            BigDecimal change = prices[i].subtract(prices[i - 1]);
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains = gains.add(change);
            } else {
                losses = losses.add(change.abs());
            }
        }

        BigDecimal avgGain = gains.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = losses.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP)
        );

        return rsi.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChange(BigDecimal[] prices, int daysAgo) {
        if (prices.length < daysAgo + 1) return null;

        BigDecimal old = prices[prices.length - daysAgo - 1];
        BigDecimal current = prices[prices.length - 1];

        if (old.compareTo(BigDecimal.ZERO) == 0) return null;

        return current.subtract(old)
                .divide(old, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVolatility(BigDecimal[] prices, int period) {
        if (prices.length < period) return null;

        double[] vals = new double[period];
        double sum = 0;
        for (int i = 0; i < period; i++) {
            vals[i] = prices[prices.length - period + i].doubleValue();
            sum += vals[i];
        }
        double mean = sum / period;
        if (mean == 0) return null;

        double variance = 0;
        for (double v : vals) {
            variance += (v - mean) * (v - mean);
        }
        variance /= period;
        double stdDev = Math.sqrt(variance);

        double volatilityPercent = (stdDev / mean) * 100;
        return BigDecimal.valueOf(volatilityPercent).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildMaSignal(BigDecimal price, BigDecimal sma20, BigDecimal sma50) {
        if (sma20 == null) return "VERI_YOK";
        boolean aboveSma20 = price.compareTo(sma20) > 0;
        boolean aboveSma50 = sma50 != null && price.compareTo(sma50) > 0;

        if (aboveSma20 && aboveSma50) return "ABOVE_BOTH";
        if (!aboveSma20 && (sma50 == null || !aboveSma50)) return "BELOW_BOTH";
        return "MIXED";
    }

    private String buildRsiSignal(BigDecimal rsi) {
        if (rsi == null) return "VERI_YOK";
        double r = rsi.doubleValue();
        if (r >= 70) return "OVERBOUGHT";
        if (r <= 30) return "OVERSOLD";
        return "NEUTRAL";
    }

    private String buildMomentumSignal(BigDecimal change7d, BigDecimal change30d) {
        if (change7d == null) return "VERI_YOK";
        double c7 = change7d.doubleValue();
        double c30 = change30d != null ? change30d.doubleValue() : 0;

        if (c7 > 5 && c30 > 0) return "BULLISH";
        if (c7 < -5 && c30 < 0) return "BEARISH";
        return "NEUTRAL";
    }

    private String buildVolatilityLevel(BigDecimal vol) {
        if (vol == null) return "VERI_YOK";
        double v = vol.doubleValue();
        if (v < 2) return "DUSUK";
        if (v < 5) return "ORTA";
        return "YUKSEK";
    }

    private String buildOverallTrend(BigDecimal price, BigDecimal sma20, BigDecimal sma50,
                                       BigDecimal rsi, BigDecimal change30d) {
        int score = 0;

        if (sma20 != null && price.compareTo(sma20) > 0) score++;
        else if (sma20 != null) score--;

        if (sma50 != null && price.compareTo(sma50) > 0) score++;
        else if (sma50 != null) score--;

        if (change30d != null) {
            if (change30d.compareTo(BigDecimal.valueOf(5)) > 0) score++;
            else if (change30d.compareTo(BigDecimal.valueOf(-5)) < 0) score--;
        }

        if (rsi != null) {
            double r = rsi.doubleValue();
            if (r >= 55 && r < 70) score++;
            else if (r > 30 && r <= 45) score--;
        }

        if (score >= 2) return "YUKSELIS";
        if (score <= -2) return "DUSUS";
        return "YATAY";
    }

    private String buildRecommendation(String symbol, BigDecimal price, BigDecimal sma20,
                                        BigDecimal sma50, BigDecimal rsi, String rsiSignal,
                                        BigDecimal c7, BigDecimal c30, String momentum,
                                        BigDecimal vol, String volLevel, String trend) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s'in genel trendi: %s. ", symbol, trend));

        if ("OVERBOUGHT".equals(rsiSignal)) {
            sb.append(String.format("RSI %s seviyesinde, asiri alim bolgesinde. ",
                    rsi.setScale(0, RoundingMode.HALF_UP)));
            sb.append("Kisa vadede bir duzeltme veya yatay seyir gelebilir, dikkatli ol. ");
        } else if ("OVERSOLD".equals(rsiSignal)) {
            sb.append(String.format("RSI %s seviyesinde, asiri satim bolgesinde. ",
                    rsi.setScale(0, RoundingMode.HALF_UP)));
            sb.append("Yatirimcilar icin potansiyel alim firsati olusabilir, ama kesinlikle tavsiye degildir. ");
        } else if (rsi != null) {
            sb.append(String.format("RSI %s, notr bolgede. ", rsi.setScale(0, RoundingMode.HALF_UP)));
        }

        if (c30 != null) {
            sb.append(String.format("Son 30 gunde %%%.1f hareket var. ", c30.doubleValue()));
        }

        if ("YUKSEK".equals(volLevel)) {
            sb.append(String.format("Volatilite %%%.1f - yuksek seviyede, ani fiyat hareketlerine hazirlikli ol. ",
                    vol.doubleValue()));
        } else if ("DUSUK".equals(volLevel)) {
            sb.append("Volatilite dusuk, fiyat hareketleri sakin. ");
        }

        if ("BULLISH".equals(momentum)) {
            sb.append("Momentum guclu, kisa vadede yukselis devam edebilir.");
        } else if ("BEARISH".equals(momentum)) {
            sb.append("Momentum zayif, kisa vadede dususler gorebiliriz.");
        }

        return sb.toString().trim();
    }

    private TechnicalIndicatorsResponse emptyResponse(String symbol, String reason) {
        return new TechnicalIndicatorsResponse(
                symbol, null,
                null, null, "VERI_YOK",
                null, "VERI_YOK",
                null, null, "VERI_YOK",
                null, "VERI_YOK",
                "YATAY",
                reason
        );
    }
}
