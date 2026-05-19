package com.finportfolio.dto;

import java.time.Instant;

public record ChatResponse(
        String message,
        Instant timestamp
) {
    public static ChatResponse of(String message) {
        return new ChatResponse(message, Instant.now());
    }
}