package com.finportfolio.service;

import com.finportfolio.entity.PortfolioItem;
import com.finportfolio.entity.PriceSnapshot;
import com.finportfolio.repository.PortfolioItemRepository;
import com.finportfolio.repository.PriceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final MarketDataService marketDataService;

    /**
     * Kullanıcının login anındaki tüm portföy fiyatlarını snapshot olarak kaydeder.
     * Önceki snapshot'ları siler, yenilerini yazar.
     */
    @Transactional
    public void captureSnapshotForUser(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findAllByUserId(userId);
        if (items.isEmpty()) {
            log.debug("Kullanici {} icin snapshot alinmadi - portfoy bos", userId);
            return;
        }

        // Eski snapshot'lari sil
        priceSnapshotRepository.deleteAllByUserId(userId);

        Set<String> uniqueSymbols = new HashSet<>();
        Instant now = Instant.now();

        for (PortfolioItem item : items) {
            if (!uniqueSymbols.add(item.getAssetSymbol())) continue;

            BigDecimal currentPrice = marketDataService.getPriceTry(item.getAssetSymbol());
            if (currentPrice == null) {
                log.warn("Snapshot - guncel fiyat alinamadi: {}", item.getAssetSymbol());
                continue;
            }

            PriceSnapshot snapshot = PriceSnapshot.builder()
                    .userId(userId)
                    .assetSymbol(item.getAssetSymbol())
                    .priceTry(currentPrice)
                    .snapshotAt(now)
                    .build();
            @SuppressWarnings({"null", "unused"})
            var saved = priceSnapshotRepository.save(snapshot);
        }

        log.info("Kullanici {} icin {} snapshot olusturuldu", userId, uniqueSymbols.size());
    }

    /**
     * Kullanıcının önceki snapshot'larını döner.
     * Map: assetSymbol -> snapshot fiyatı
     */
    public Map<String, BigDecimal> getLastSnapshotPrices(Long userId) {
        List<PriceSnapshot> snapshots = priceSnapshotRepository.findAllByUserId(userId);
        Map<String, BigDecimal> result = new HashMap<>();
        for (PriceSnapshot snapshot : snapshots) {
            result.put(snapshot.getAssetSymbol(), snapshot.getPriceTry());
        }
        return result;
    }

    /**
     * Son snapshot tarihini döner (en eski snapshot'tan birinin tarihi).
     * Eğer hiç snapshot yoksa null döner.
     */
    public Instant getLastSnapshotTime(Long userId) {
        List<PriceSnapshot> snapshots = priceSnapshotRepository.findAllByUserId(userId);
        if (snapshots.isEmpty()) return null;
        return snapshots.get(0).getSnapshotAt();
    }
}