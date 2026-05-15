package com.finportfolio.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Altin ve gumus ons fiyatlari icin client.
 *
 * Donen veri USD/ons cinsinden.
 * 1 ons = 31.1035 gram
 * Gram altin TL = (USD/ons / 31.1035) * USD/TRY
 *
 * Ucretsiz endpoint: https://api.gold-api.com/price/XAU
 * Yanit: {"name":"Gold","price":2654.50,...}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoldClient {

    private static final BigDecimal OUNCE_TO_GRAM = new BigDecimal("31.1035");

    // Geleneksel Türk altın tipleri için gram katsayıları (saf altın gramı)
    public static final BigDecimal CEYREK_GRAM = new BigDecimal("1.75");
    public static final BigDecimal YARIM_GRAM = new BigDecimal("3.50");
    public static final BigDecimal TAM_GRAM = new BigDecimal("7.00");

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Altinin gram TL fiyatini hesaplar.
     * @param usdTryRate guncel USD/TRY kuru
     * @return gram altinin TL fiyati, hata olursa null
     */
    public BigDecimal getGramAltinTry(BigDecimal usdTryRate) {
        BigDecimal goldOunceUsd = fetchGoldOunceUsd();
        if (goldOunceUsd == null || usdTryRate == null) return null;

        // (USD/ons / 31.1035) * USD/TRY = TL/gram
        return goldOunceUsd
                .divide(OUNCE_TO_GRAM, 4, RoundingMode.HALF_UP)
                .multiply(usdTryRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gumusun gram TL fiyatini hesaplar.
     */
    public BigDecimal getGramGumusTry(BigDecimal usdTryRate) {
        BigDecimal silverOunceUsd = fetchSilverOunceUsd();
        if (silverOunceUsd == null || usdTryRate == null) return null;

        return silverOunceUsd
                .divide(OUNCE_TO_GRAM, 4, RoundingMode.HALF_UP)
                .multiply(usdTryRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Ceyrek altinin TL fiyati (1.75 gram saf altin)
     */
    public BigDecimal getCeyrekAltinTry(BigDecimal gramAltinTry) {
        if (gramAltinTry == null) return null;
        return gramAltinTry.multiply(CEYREK_GRAM).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getYarimAltinTry(BigDecimal gramAltinTry) {
        if (gramAltinTry == null) return null;
        return gramAltinTry.multiply(YARIM_GRAM).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTamAltinTry(BigDecimal gramAltinTry) {
        if (gramAltinTry == null) return null;
        return gramAltinTry.multiply(TAM_GRAM).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal fetchGoldOunceUsd() {
        try {
            String response = restClient.get()
                    .uri("https://api.gold-api.com/price/XAU")
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            String price = json.path("price").asText();
            return new BigDecimal(price);
        } catch (Exception e) {
            log.warn("Altin ons fiyati alinamadi: {}", e.getMessage());
            // Yedek deger - acil durumlarda kullan
            return new BigDecimal("2650.00");
        }
    }

    private BigDecimal fetchSilverOunceUsd() {
        try {
            String response = restClient.get()
                    .uri("https://api.gold-api.com/price/XAG")
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            String price = json.path("price").asText();
            return new BigDecimal(price);
        } catch (Exception e) {
            log.warn("Gumus ons fiyati alinamadi: {}", e.getMessage());
            return new BigDecimal("31.50"); // Yedek
        }
    }
}