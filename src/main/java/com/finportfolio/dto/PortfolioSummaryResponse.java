package com.finportfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryResponse(
        List<PortfolioItemResponse> items,
        BigDecimal totalInvestedTry,
        BigDecimal totalCurrentValueTry,
        BigDecimal totalProfitLossTry,
        BigDecimal totalProfitLossPercent
) {}