package com.finportfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulationRequest(
        @NotBlank
        String assetSymbol,

        @NotNull
        @DecimalMin(value = "1.0", message = "En az 1 TL yatirim girilmeli")
        BigDecimal investmentTry,

        @NotNull
        @PastOrPresent(message = "Tarih bugun veya gecmiste olmali")
        LocalDate investmentDate
) {}