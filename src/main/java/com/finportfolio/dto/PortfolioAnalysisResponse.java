package com.finportfolio.dto;

import com.finportfolio.entity.AssetType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PortfolioAnalysisResponse(
        // Genel
        BigDecimal totalValueTry,
        int totalAssets,

        // Dağılım
        Map<AssetType, AssetAllocation> allocation,

        // Risk
        int riskScore,             // 0-100
        String riskLevel,          // DUSUK, ORTA, YUKSEK, COK_YUKSEK
        String riskColor,          // green, yellow, orange, red
        BigDecimal diversificationScore,  // 0-100, yuksek = iyi

        // Öneriler
        List<String> recommendations,
        List<String> warnings
) {
    public record AssetAllocation(
            BigDecimal totalValueTry,
            BigDecimal percent,
            int itemCount
    ) {}
}