package com.finportfolio.config;

import com.finportfolio.websocket.PriceStreamHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PriceStreamHandler priceStreamHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Sadece izin verilen origin'ler WebSocket'e baglanabilir
        registry.addHandler(priceStreamHandler, "/ws/prices")
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}