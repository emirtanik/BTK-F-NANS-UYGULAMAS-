package com.finportfolio.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finportfolio.dto.CandleResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kripto fiyat client'i - Java native HttpClient kullanir.
 * CoinGecko API: Turkiye'den erisilebilir, key gerektirmez, TL fiyat doner.
 * Sinif adi BinanceClient olarak korundu (geri uyum), ama CoinGecko ile calisir.
 */
@Component
@Slf4j
public class BinanceClient {

    private static final Map<String, String> COIN_ID_MAP = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "BNB", "binancecoin",
            "SOL", "solana",
            "XRP", "ripple",
            "ADA", "cardano"
    );

    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public BigDecimal getPriceUsdt(String symbol) {
        String coinId = COIN_ID_MAP.get(symbol.toUpperCase());
        if (coinId == null) return null;

        try {
            String url = "https://api.coingecko.com/api/v3/simple/price?ids="
                    + coinId + "&vs_currencies=try";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "FinPortfolio/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("CoinGecko fiyat ({}) status: {}", symbol, response.statusCode());
                return null;
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode priceNode = json.path(coinId).path("try");
            if (priceNode.isMissingNode()) return null;
            return new BigDecimal(priceNode.asText());
        } catch (Exception e) {
            log.warn("CoinGecko fiyat alinamadi ({}): {}", symbol, e.getMessage());
            return null;
        }
    }

    public Map<String, BigDecimal> getAllPricesUsdt() {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            String idsParam = String.join(",", COIN_ID_MAP.values());
            String url = "https://api.coingecko.com/api/v3/simple/price?ids="
                    + idsParam + "&vs_currencies=try";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "FinPortfolio/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("CoinGecko toplu fiyat status: {}", response.statusCode());
                return result;
            }

            JsonNode json = objectMapper.readTree(response.body());
            for (Map.Entry<String, String> entry : COIN_ID_MAP.entrySet()) {
                JsonNode priceNode = json.path(entry.getValue()).path("try");
                if (!priceNode.isMissingNode()) {
                    result.put(entry.getKey(), new BigDecimal(priceNode.asText()));
                }
            }
            log.info("CoinGecko'dan {} kripto fiyati alindi", result.size());
        } catch (Exception e) {
            log.error("CoinGecko toplu fiyat alinamadi: {}", e.getMessage());
        }

        return result;
    }

    public List<CandleResponse> getKlines(String symbol, String interval, int limit) {
        String coinId = COIN_ID_MAP.get(symbol.toUpperCase());
        if (coinId == null) return List.of();

        int days = switch (interval) {
            case "1h", "2h", "4h" -> {
                int h = Integer.parseInt(interval.replace("h", ""));
                yield Math.max(1, (limit * h) / 24);
            }
            case "1d" -> Math.min(limit, 365);
            case "1w" -> Math.min(limit * 7, 365);
            default -> 7;
        };
        if (days < 1) days = 1;
        if (days > 365) days = 365;

        try {
            String url = "https://api.coingecko.com/api/v3/coins/" + coinId
                    + "/market_chart?vs_currency=try&days=" + days;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "FinPortfolio/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("CoinGecko kline status ({}): {}", symbol, response.statusCode());
                return List.of();
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode prices = json.path("prices");
            if (!prices.isArray()) return List.of();

            List<CandleResponse> result = new ArrayList<>();
            int total = prices.size();
            int step = Math.max(1, total / Math.max(1, limit));

            for (int i = 0; i < total; i += step) {
                JsonNode pricePoint = prices.get(i);
                if (pricePoint == null || pricePoint.size() < 2) continue;
                long timestampMs = pricePoint.get(0).asLong();
                BigDecimal price = new BigDecimal(pricePoint.get(1).asText());
                result.add(new CandleResponse(
                        timestampMs / 1000, price, price, price, price, BigDecimal.ZERO));
            }

            return result;
        } catch (Exception e) {
            log.warn("CoinGecko kline alinamadi ({}): {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
