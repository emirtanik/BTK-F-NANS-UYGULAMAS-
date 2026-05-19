package com.finportfolio.dto;

import java.math.BigDecimal;

public record PriceResponse(
        String symbol,
        BigDecimal priceTry
) {}