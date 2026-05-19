package com.finportfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Column(name = "price_try", nullable = false, precision = 24, scale = 4)
    private BigDecimal priceTry;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @PrePersist
    void onCreate() {
        if (snapshotAt == null) snapshotAt = Instant.now();
    }
}