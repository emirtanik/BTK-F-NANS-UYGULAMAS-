package com.finportfolio.dto;

import com.finportfolio.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String assetSymbol,
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getAssetSymbol(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt()
        );
    }
}