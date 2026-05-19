package com.finportfolio.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Doviz kuru client.
 * Ucretsiz endpoint: https://open.er-api.com/v6/latest/USD
 * Key gerektirmez. Tum kurlari USD baz alarak doner.
 *
 * Bizim ihtiyacimiz: USD/TRY, EUR/TRY, GBP/TRY
 * API USD bazli verdigi icin:
 *   USD/TRY = response.rates.TRY (direkt)
 *   EUR/TRY = (USD/TRY) / (USD/EUR) = TRY / (1/EUR rate) ... aslinda EUR/USD * USD/TRY
 *   Daha kolay: TRY = 1 USD * USD/TRY rate
 *               EUR'a karsi TRY: 1 EUR = ? TRY => (1/USD_EUR_rate) * USD_TRY_rate
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateClient {

    private static final String URL = "https://open.er-api.com/v6/latest/USD";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tum kurlari USD bazinda doner.
     * Donen Map: {"TRY": 39.85, "EUR": 0.92, "GBP": 0.79, ...}
     */
    public Map<String, BigDecimal> getAllRatesFromUsd() {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            String response = restClient.get()
                    .uri(URL)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            JsonNode rates = json.path("rates");

            if (rates.isObject()) {
                rates.fields().forEachRemaining(entry -> {
                    String currency = entry.getKey();
                    BigDecimal rate = new BigDecimal(entry.getValue().asText());
                    result.put(currency, rate);
                });
            }
        } catch (Exception e) {
            log.error("Doviz kurlari alinamadi: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 1 [currency] kac TL eder?
     * Ornek: getRateToTry("USD") -> 39.85
     *        getRateToTry("EUR") -> 43.20
     */
    public BigDecimal getRateToTry(String currency) {
        Map<String, BigDecimal> rates = getAllRatesFromUsd();
        BigDecimal tryRate = rates.get("TRY");

        if (tryRate == null) {
            log.warn("TRY kuru bulunamadi");
            return null;
        }

        if ("USD".equalsIgnoreCase(currency)) {
            return tryRate;  // 1 USD kac TL
        }

        BigDecimal currencyRate = rates.get(currency.toUpperCase());
        if (currencyRate == null) {
            log.warn("Kur bulunamadi: {}", currency);
            return null;
        }

        // 1 [currency] = (1 / currencyRate) USD, bu da (1/currencyRate * tryRate) TL eder
        // Ornek: EUR rate 0.92 ise, 1 EUR = (1/0.92) USD = 1.087 USD = 1.087 * 39.85 = 43.30 TL
        return tryRate.divide(currencyRate, 4, java.math.RoundingMode.HALF_UP);
    }
}