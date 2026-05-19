package com.finportfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String model;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gemini API'ye prompt gonderir, donen metni geri verir.
     * API anahtari URL query parameter yerine x-goog-api-key header'inda gonderilir
     * (URL'ler proxy/access loglarinda gozukur, header'lar gozukmez).
     */
    public String generate(String systemInstruction, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Gemini API key ayarlanmamis. GEMINI_API_KEY env variable'ini set edin.");
            return "AI servisi henuz yapilandirilmamis.";
        }

        try {
            String url = String.format("%s/models/%s:generateContent", baseUrl, model);

            ObjectNode root = objectMapper.createObjectNode();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                ObjectNode systemNode = objectMapper.createObjectNode();
                ArrayNode systemParts = objectMapper.createArrayNode();
                ObjectNode systemPart = objectMapper.createObjectNode();
                systemPart.put("text", systemInstruction);
                systemParts.add(systemPart);
                systemNode.set("parts", systemParts);
                root.set("systemInstruction", systemNode);
            }

            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", userMessage);
            userParts.add(userPart);
            userContent.set("parts", userParts);
            contents.add(userContent);
            root.set("contents", contents);

            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 800);
            root.set("generationConfig", generationConfig);

            RestClient client = RestClient.create();
            @SuppressWarnings("null")
            String responseBody = client.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("x-goog-api-key", apiKey)
                    .body(root.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode candidates = response.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode text = candidates.get(0).path("content").path("parts").get(0).path("text");
                if (!text.isMissingNode()) {
                    return text.asText().trim();
                }
            }

            log.warn("Gemini bos yanit dondu");
            return "Uzgunum, su an cevap uretemedim. Lutfen tekrar dene.";

        } catch (Exception e) {
            // Stack trace'i logla ama API key sizdirma riskine karsi mesaji generic tut
            log.error("Gemini API hatasi: {}", e.getClass().getSimpleName());
            return "Su an asistana ulasamiyorum. Lutfen birazdan tekrar dene.";
        }
    }
}