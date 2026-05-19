package com.finportfolio.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private AlertCondition conditionType;

    @Column(name = "threshold_value", nullable = false, precision = 24, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_triggered", nullable = false)
    @Builder.Default
    private Boolean isTriggered = false;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "triggered_price", precision = 24, scale = 4)
    private BigDecimal triggeredPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}