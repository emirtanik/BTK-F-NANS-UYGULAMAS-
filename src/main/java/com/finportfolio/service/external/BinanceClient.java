package com.finportfolio.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finportfolio.dto.CandleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binance public API client.
 * Key gerektirmez. Kripto fiyatlari ve tarihsel mum verisi icin kullanilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceClient {

    // Bizim sembolumuz -> Binance'in sembolu
    private static final Map<String, String> SYMBOL_MAP = Map.of(
            "BTC", "BTCUSDT",
            "ETH", "ETHUSDT",
            "BNB", "BNBUSDT",
            "SOL", "SOLUSDT",
            "XRP", "XRPUSDT",
            "ADA", "ADAUSDT"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tek bir kripto'nun USDT fiyatini doner.
     */
    public BigDecimal getPriceUsdt(String symbol) {
        String binanceSymbol = SYMBOL_MAP.get(symbol.toUpperCase());
        if (binanceSymbol == null) {
            log.warn("Binance'de desteklenmeyen sembol: {}", symbol);
            return null;
        }

        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.binance.com")
                            .path("/api/v3/ticker/price")
                            .queryParam("symbol", binanceSymbol)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            String price = json.path("price").asText();
            return new BigDecimal(price);
        } catch (Exception e) {
            log.warn("Binance fiyat alinamadi ({}): {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Birden fazla kripto fiyatini tek seferde ceker.
     */
    public Map<String, BigDecimal> getAllPricesUsdt() {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            StringBuilder symbolsParam = new StringBuilder("[");
            List<String> binanceSymbols = SYMBOL_MAP.values().stream().toList();
            for (int i = 0; i < binanceSymbols.size(); i++) {
                if (i > 0) symbolsParam.append(",");
                symbolsParam.append("\"").append(binanceSymbols.get(i)).append("\"");
            }
            symbolsParam.append("]");

            final String symbolsParamFinal = symbolsParam.toString();

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.binance.com")
                            .path("/api/v3/ticker/price")
                            .queryParam("symbols", symbolsParamFinal)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode array = objectMapper.readTree(response);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String binanceSymbol = node.path("symbol").asText();
                    String priceStr = node.path("price").asText();

                    for (Map.Entry<String, String> entry : SYMBOL_MAP.entrySet()) {
                        if (entry.getValue().equals(binanceSymbol)) {
                            result.put(entry.getKey(), new BigDecimal(priceStr));
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Binance toplu fiyat alinamadi: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Bir kripto'nun tarihsel mum verisini (OHLCV) doner.
     *
     * @param symbol BTC, ETH gibi
     * @param interval 1m, 5m, 15m, 1h, 4h, 1d, 1w
     * @param limit kac mum dondursun (max 1000)
     */
    public List<CandleResponse> getKlines(String symbol, String interval, int limit) {
        String binanceSymbol = SYMBOL_MAP.get(symbol.toUpperCase());
        if (binanceSymbol == null) {
            log.warn("Binance'de desteklenmeyen sembol: {}", symbol);
            return List.of();
        }

        final int safeLimit = Math.min(limit, 1000);
        final String safeInterval = interval;

        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.binance.com")
                            .path("/api/v3/klines")
                            .queryParam("symbol", binanceSymbol)
                            .queryParam("interval", safeInterval)
                            .queryParam("limit", safeLimit)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode array = objectMapper.readTree(response);
            List<CandleResponse> result = new ArrayList<>();

            if (array.isArray()) {
                for (JsonNode kline : array) {
                    // Binance kline formati: [openTime, open, high, low, close, volume, closeTime, ...]
                    long openTimeMs = kline.get(0).asLong();
                    BigDecimal open = new BigDecimal(kline.get(1).asText());
                    BigDecimal high = new BigDecimal(kline.get(2).asText());
                    BigDecimal low = new BigDecimal(kline.get(3).asText());
                    BigDecimal close = new BigDecimal(kline.get(4).asText());
                    BigDecimal volume = new BigDecimal(kline.get(5).asText());

                    result.add(new CandleResponse(
                            openTimeMs / 1000, // ms -> sn
                            open, high, low, close, volume
                    ));
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("Binance kline alinamadi ({}): {}", symbol, e.getMessage());
            return List.of();
        }
    }
}