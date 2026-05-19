package com.finportfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record WelcomeChatResponse(
        String botMessage,
        List<AssetChange> changes,
        BigDecimal totalProfitLossTry,
        BigDecimal totalProfitLossPercent
) {
    public record AssetChange(
            String symbol,
            BigDecimal oldPriceTry,
            BigDecimal currentPriceTry,
            BigDecimal changePercent,
            BigDecimal profitLossTry,
            BigDecimal userAmount
    ) {}
}