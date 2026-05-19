package com.finportfolio.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finportfolio.dto.PriceResponse;
import com.finportfolio.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler - tum bagli istemcilere fiyat akisi yapar.
 *
 * Akis:
 *   1. Istemci /ws/prices'a baglanir
 *   2. Bagli istemciler set'e eklenir
 *   3. Her 5 saniyede bir tum istemcilere guncel fiyatlar gonderilir
 *   4. Istemci ayrildiginda set'ten cikarilir
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PriceStreamHandler extends TextWebSocketHandler {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Bagli tum WebSocket oturumlari (thread-safe set)
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(@org.springframework.lang.NonNull WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket baglandi: {} (toplam aktif: {})", session.getId(), sessions.size());

        // Yeni baglanan istemciye hemen guncel fiyatlari gonder
        sendPricesTo(session);
    }

    @Override
    public void afterConnectionClosed(@org.springframework.lang.NonNull WebSocketSession session, @org.springframework.lang.NonNull CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket koptu: {} (toplam aktif: {})", session.getId(), sessions.size());
    }

    /**
     * Her 5 saniyede bir tum bagli istemcilere fiyat gonderir.
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastPrices() {
        if (sessions.isEmpty()) return;

        try {
            List<PriceResponse> prices = marketDataService.getAllPrices();
            @SuppressWarnings("null")
            String json = objectMapper.writeValueAsString(prices);
            @SuppressWarnings("null")
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("Mesaj gonderilemedi {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fiyat broadcast hatasi", e);
        }
    }

    private void sendPricesTo(WebSocketSession session) {
        try {
            List<PriceResponse> prices = marketDataService.getAllPrices();
            @SuppressWarnings("null")
            String json = objectMapper.writeValueAsString(prices);
            @SuppressWarnings("null")
            var msg = new TextMessage(json);
            session.sendMessage(msg);
        } catch (Exception e) {
            log.warn("Ilk fiyat gonderilemedi: {}", e.getMessage());
        }
    }
}