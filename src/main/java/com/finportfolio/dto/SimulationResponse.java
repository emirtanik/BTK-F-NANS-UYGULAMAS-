package com.finportfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulationResponse(
        String assetSymbol,
        LocalDate investmentDate,
        BigDecimal investmentTry,
        BigDecimal historicalPriceTry,
        BigDecimal currentPriceTry,
        BigDecimal amountBought,
        BigDecimal currentValueTry,
        BigDecimal profitLossTry,
        BigDecimal profitLossPercent,
        long daysHeld,
        String summary
) {}