package com.finportfolio.dto;

import com.finportfolio.entity.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioItemRequest(
        @NotNull AssetType assetType,

        @NotBlank
        String assetSymbol,

        @NotNull
        @DecimalMin(value = "0.00000001", message = "Miktar 0'dan buyuk olmali")
        BigDecimal amount,

        @NotNull
        @DecimalMin(value = "0.0001", message = "Alis fiyati 0'dan buyuk olmali")
        BigDecimal buyPriceTry,

        Instant buyDate  // null gelirse "şimdi" alınır
) {}