package com.finportfolio.service;

import com.finportfolio.dto.PortfolioAnalysisResponse;
import com.finportfolio.entity.AssetType;
import com.finportfolio.entity.PortfolioItem;
import com.finportfolio.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortfolioAnalysisService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final MarketDataService marketDataService;

    public PortfolioAnalysisResponse analyze(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findAllByUserId(userId);

        if (items.isEmpty()) {
            return new PortfolioAnalysisResponse(
                    BigDecimal.ZERO, 0,
                    Map.of(),
                    0, "BOS", "gray", BigDecimal.ZERO,
                    List.of("Henuz portfoyunde varlik yok. Altin, doviz veya kripto yatirimlari ekleyerek baslayabilirsin."),
                    List.of()
            );
        }

        // 1. Toplam değeri ve tip bazında dağılımı hesapla
        Map<AssetType, BigDecimal> valueByType = new HashMap<>();
        Map<AssetType, Integer> countByType = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal currentPrice = marketDataService.getPriceTry(item.getAssetSymbol());
            if (currentPrice == null) continue;

            BigDecimal value = currentPrice.multiply(item.getAmount());
            totalValue = totalValue.add(value);

            valueByType.merge(item.getAssetType(), value, BigDecimal::add);
            @SuppressWarnings({"null", "unused"})
            Integer count = countByType.merge(item.getAssetType(), 1, Integer::sum);
        }

        // 2. Yüzde dağılımları
        Map<AssetType, PortfolioAnalysisResponse.AssetAllocation> allocation = new HashMap<>();
        for (Map.Entry<AssetType, BigDecimal> entry : valueByType.entrySet()) {
            BigDecimal percent = BigDecimal.ZERO;
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                percent = entry.getValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            allocation.put(entry.getKey(), new PortfolioAnalysisResponse.AssetAllocation(
                    entry.getValue().setScale(2, RoundingMode.HALF_UP),
                    percent,
                    countByType.getOrDefault(entry.getKey(), 0)
            ));
        }

        // 3. Risk skoru hesapla
        int riskScore = calculateRiskScore(allocation, items.size());

        // 4. Risk seviyesi
        String riskLevel;
        String riskColor;
        if (riskScore < 25) {
            riskLevel = "DUSUK";
            riskColor = "green";
        } else if (riskScore < 50) {
            riskLevel = "ORTA";
            riskColor = "yellow";
        } else if (riskScore < 75) {
            riskLevel = "YUKSEK";
            riskColor = "orange";
        } else {
            riskLevel = "COK_YUKSEK";
            riskColor = "red";
        }

        // 5. Çeşitlendirme skoru (Herfindahl-Hirschman index ters)
        BigDecimal diversificationScore = calculateDiversificationScore(allocation);

        // 6. Öneri ve uyarılar
        List<String> recommendations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BigDecimal cryptoPercent = getPercent(allocation, AssetType.CRYPTO);
        BigDecimal goldPercent = getPercent(allocation, AssetType.GOLD);
        BigDecimal silverPercent = getPercent(allocation, AssetType.SILVER);
        BigDecimal currencyPercent = getPercent(allocation, AssetType.CURRENCY);

        // Kripto agirligi
        if (cryptoPercent.compareTo(BigDecimal.valueOf(60)) > 0) {
            warnings.add(String.format("Portfoyunun %%%.1f'i kriptoda. Bu cok yuksek bir oran, volatiliteye dikkat et.", cryptoPercent.doubleValue()));
        } else if (cryptoPercent.compareTo(BigDecimal.valueOf(40)) > 0) {
            recommendations.add(String.format("Kripto agirligi %%%.1f. Onerilen %%10-20 araligi icin altin veya doviz pozisyonunu artirmayi dusunebilirsin.", cryptoPercent.doubleValue()));
        }

        // Cesitlendirme yetersiz
        if (allocation.size() == 1) {
            warnings.add("Portfoyun tek bir varlik sinifinda yogunlasmis. Farkli varlik tiplerine dagilmayi dene.");
        } else if (allocation.size() == 2) {
            recommendations.add("Iki farkli varlik sinifin var, bu iyi bir baslangic. Altin veya doviz ekleyerek dengeyi guclendirebilirsin.");
        }

        // Altin yok
        if (goldPercent.compareTo(BigDecimal.ZERO) == 0 && silverPercent.compareTo(BigDecimal.ZERO) == 0) {
            recommendations.add("Portfoyunde kiymetli maden yok. Enflasyona karsi koruma icin altin veya gumus eklemeyi dusunebilirsin.");
        }

        // Doviz yok
        if (currencyPercent.compareTo(BigDecimal.ZERO) == 0) {
            recommendations.add("Doviz pozisyonun yok. Kur riskini dengelemek icin dolar veya euro eklemek mantikli olabilir.");
        }

        // Tek bir varliga konsantrasyon
        if (items.size() == 1) {
            warnings.add("Sadece 1 yatirimin var. Tek varliga bagli kalmak yuksek risk tasir.");
        }

        // Eger her sey ideal
        if (recommendations.isEmpty() && warnings.isEmpty()) {
            recommendations.add("Portfoyun dengeli gorunuyor, bu sekilde devam et. Piyasa hareketlerine gore periyodik olarak gozden gecirmeyi unutma.");
        }

        return new PortfolioAnalysisResponse(
                totalValue.setScale(2, RoundingMode.HALF_UP),
                items.size(),
                allocation,
                riskScore,
                riskLevel,
                riskColor,
                diversificationScore,
                recommendations,
                warnings
        );
    }

    /**
     * Risk skoru hesabi - basit ama anlamli formul:
     * - Her varlik tipinin onerilen agirlika gore sapmasi
     * - Konsantrasyon (HHI)
     * - Kripto agirligi (yuksek risk faktoru)
     */
    private int calculateRiskScore(Map<AssetType, PortfolioAnalysisResponse.AssetAllocation> allocation, int itemCount) {
        double score = 0;

        // Kripto risk faktoru (her %1 kripto = 0.7 puan)
        BigDecimal cryptoPct = getPercent(allocation, AssetType.CRYPTO);
        score += cryptoPct.doubleValue() * 0.7;

        // Konsantrasyon risk (tek varliga bagli olma)
        // HHI = sum(percent^2). Tum portfoy tek varlikta = 10000, esit dagilmis 4 varlik = 2500
        double hhi = 0;
        for (PortfolioAnalysisResponse.AssetAllocation a : allocation.values()) {
            double p = a.percent().doubleValue();
            hhi += p * p;
        }
        // HHI'yi 0-30 araligina cevir (10000 -> 30, 2500 -> 7.5)
        score += (hhi / 10000.0) * 30;

        // Tek itemli portfoy ekstra ceza
        if (itemCount == 1) score += 15;

        // Cap at 100
        return (int) Math.min(100, Math.max(0, score));
    }

    /**
     * Cesitlendirme skoru: HHI'nin tersi
     * Tek varlik = 0 (kotu), esit dagilmis 4 varlik = ~75 (iyi)
     */
    private BigDecimal calculateDiversificationScore(Map<AssetType, PortfolioAnalysisResponse.AssetAllocation> allocation) {
        if (allocation.isEmpty()) return BigDecimal.ZERO;

        double hhi = 0;
        for (PortfolioAnalysisResponse.AssetAllocation a : allocation.values()) {
            double p = a.percent().doubleValue();
            hhi += p * p;
        }

        // HHI = 10000 (en kotu) -> 0
        // HHI = 2500 (4 esit varlik) -> 75
        // HHI = 1000 (10 esit varlik) -> 90
        double score = 100 - (hhi / 100.0);
        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getPercent(Map<AssetType, PortfolioAnalysisResponse.AssetAllocation> allocation, AssetType type) {
        return allocation.containsKey(type) ? allocation.get(type).percent() : BigDecimal.ZERO;
    }
}