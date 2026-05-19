package com.finportfolio.dto;

import java.math.BigDecimal;

public record TechnicalIndicatorsResponse(
        String symbol,
        BigDecimal currentPrice,

        BigDecimal sma20,
        BigDecimal sma50,
        String maSignal,

        BigDecimal rsi14,
        String rsiSignal,

        BigDecimal change7d,
        BigDecimal change30d,
        String momentumSignal,

        BigDecimal volatility,
        String volatilityLevel,

        String overallTrend,
        String recommendation
) {}
