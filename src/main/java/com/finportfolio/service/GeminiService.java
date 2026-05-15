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
     * Gemini API'ye prompt gönderir, dönen metni geri verir.
     * systemInstruction → asistan kişiliği (Türkçe finans danışmanı)
     * userMessage → kullanıcının mesajı veya context'li soru
     */
    public String generate(String systemInstruction, String userMessage) {
        try {
            String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

            // Request body JSON oluştur
            ObjectNode root = objectMapper.createObjectNode();

            // System instruction (asistan kişiliği)
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                ObjectNode systemNode = objectMapper.createObjectNode();
                ArrayNode systemParts = objectMapper.createArrayNode();
                ObjectNode systemPart = objectMapper.createObjectNode();
                systemPart.put("text", systemInstruction);
                systemParts.add(systemPart);
                systemNode.set("parts", systemParts);
                root.set("systemInstruction", systemNode);
            }

            // User content
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

            // Generation config
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 800);
            root.set("generationConfig", generationConfig);

            // HTTP isteği
            RestClient client = RestClient.create();
            String responseBody = client.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(root.toString())
                    .retrieve()
                    .body(String.class);

            // Dönen JSON'dan text alanını çıkar
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode candidates = response.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode text = candidates.get(0).path("content").path("parts").get(0).path("text");
                if (!text.isMissingNode()) {
                    return text.asText().trim();
                }
            }

            log.warn("Gemini boş yanıt döndü: {}", responseBody);
            return "Üzgünüm, şu an cevap üretemedim. Lütfen tekrar dene.";

        } catch (Exception e) {
            log.error("Gemini API hatası", e);
            return "Şu an asistana ulaşamıyorum. Lütfen birazdan tekrar dene.";
        }
    }
    private String buildFallbackResponse(String userMessage) {
}
}