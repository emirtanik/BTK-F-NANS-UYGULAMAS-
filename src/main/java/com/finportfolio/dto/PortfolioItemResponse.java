package com.finportfolio.dto;

import com.finportfolio.entity.AssetType;
import com.finportfolio.entity.PortfolioItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record PortfolioItemResponse(
        Long id,
        AssetType assetType,
        String assetSymbol,
        BigDecimal amount,
        BigDecimal buyPriceTry,
        BigDecimal currentPriceTry,
        BigDecimal investedValueTry,    // amount * buyPrice
        BigDecimal currentValueTry,     // amount * currentPrice
        BigDecimal profitLossTry,       // currentValue - investedValue
        BigDecimal profitLossPercent,
        Instant buyDate
) {
    public static PortfolioItemResponse from(PortfolioItem item, BigDecimal currentPrice) {
        BigDecimal amount = item.getAmount();
        BigDecimal buyPrice = item.getBuyPriceTry();

        BigDecimal investedValue = buyPrice.multiply(amount);
        BigDecimal currentValue = currentPrice != null
                ? currentPrice.multiply(amount)
                : BigDecimal.ZERO;
        BigDecimal profitLoss = currentValue.subtract(investedValue);

        BigDecimal profitLossPercent = BigDecimal.ZERO;
        if (investedValue.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercent = profitLoss
                    .divide(investedValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioItemResponse(
                item.getId(),
                item.getAssetType(),
                item.getAssetSymbol(),
                amount,
                buyPrice,
                currentPrice,
                investedValue,
                currentValue,
                profitLoss,
                profitLossPercent,
                item.getBuyDate()
        );
    }
}