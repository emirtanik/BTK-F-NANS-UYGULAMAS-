package com.finportfolio.dto;

import com.finportfolio.entity.AlertCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AlertRequest(
        @NotBlank String assetSymbol,
        @NotNull AlertCondition conditionType,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal thresholdValue
) {}