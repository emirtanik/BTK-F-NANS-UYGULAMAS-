package com.finportfolio.service;

import com.finportfolio.dto.AlertRequest;
import com.finportfolio.dto.NotificationResponse;
import com.finportfolio.entity.Notification;
import com.finportfolio.entity.PriceAlert;
import com.finportfolio.exception.ResourceNotFoundException;
import com.finportfolio.exception.ForbiddenException;
import com.finportfolio.repository.NotificationRepository;
import com.finportfolio.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final NotificationRepository notificationRepository;
    private final MarketDataService marketDataService;

    @Transactional
    public PriceAlert createAlert(Long userId, AlertRequest request) {
        PriceAlert alert = PriceAlert.builder()
                .userId(userId)
                .assetSymbol(request.assetSymbol().toUpperCase())
                .conditionType(request.conditionType())
                .thresholdValue(request.thresholdValue())
                .isActive(true)
                .isTriggered(false)
                .build();
        @SuppressWarnings("null")
        PriceAlert saved = priceAlertRepository.save(alert);
        return saved;
    }

    public List<PriceAlert> getUserAlerts(Long userId) {
        return priceAlertRepository.findAllByUserId(userId);
    }

    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        @SuppressWarnings("null")
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm bulunamadi"));
        if (!alert.getUserId().equals(userId)) {
            throw new ForbiddenException("Bu alarm size ait degil");
        }
        priceAlertRepository.delete(alert);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        @SuppressWarnings("null")
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim bulunamadi"));
        if (!n.getUserId().equals(userId)) {
            throw new ForbiddenException("Bu bildirim size ait degil");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        @SuppressWarnings({"null", "unused"})
        var saved = notificationRepository.saveAll(unread);
    }

    /**
     * Her 5 dakikada bir tum aktif alarmlari tarar.
     * Esik asilan alarmlar icin bildirim olusturur ve alarmi triggered isaretler.
     */
    @Scheduled(fixedRate = 300_000) // 5 dakika = 300.000 ms
    @Transactional
    public void scanAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findAllByIsActiveTrueAndIsTriggeredFalse();

        if (activeAlerts.isEmpty()) {
            return;
        }

        log.info("Alarm taramasi basliyor - {} aktif alarm", activeAlerts.size());
        int triggeredCount = 0;

        for (PriceAlert alert : activeAlerts) {
            try {
                BigDecimal currentPrice = marketDataService.getPriceTry(alert.getAssetSymbol());
                if (currentPrice == null) continue;

                if (shouldTrigger(alert, currentPrice)) {
                    triggerAlert(alert, currentPrice);
                    triggeredCount++;
                }
            } catch (Exception e) {
                log.warn("Alarm taranamadi (id={}): {}", alert.getId(), e.getMessage());
            }
        }

        if (triggeredCount > 0) {
            log.info("Alarm tarama bitti - {} alarm tetiklendi", triggeredCount);
        }
    }

    private boolean shouldTrigger(PriceAlert alert, BigDecimal currentPrice) {
        BigDecimal threshold = alert.getThresholdValue();

        return switch (alert.getConditionType()) {
            case PRICE_ABOVE -> currentPrice.compareTo(threshold) >= 0;
            case PRICE_BELOW -> currentPrice.compareTo(threshold) <= 0;
            case PERCENT_CHANGE_UP, PERCENT_CHANGE_DOWN -> false; // Daha sonra eklenebilir
        };
    }

    private void triggerAlert(PriceAlert alert, BigDecimal currentPrice) {
        // Alarmi triggered yap
        alert.setIsTriggered(true);
        alert.setTriggeredAt(Instant.now());
        alert.setTriggeredPrice(currentPrice);
        alert.setIsActive(false);
        priceAlertRepository.save(alert);

        // Bildirim olustur
        String title = String.format("Fiyat alarmi: %s", alert.getAssetSymbol());
        String message = buildAlertMessage(alert, currentPrice);

        Notification notification = Notification.builder()
                .userId(alert.getUserId())
                .title(title)
                .message(message)
                .assetSymbol(alert.getAssetSymbol())
                .isRead(false)
                .build();
        @SuppressWarnings({"null", "unused"})
        var saved = notificationRepository.save(notification);

        log.info("Alarm tetiklendi: kullanici={}, sembol={}, fiyat={}",
                alert.getUserId(), alert.getAssetSymbol(), currentPrice);
    }

    private String buildAlertMessage(PriceAlert alert, BigDecimal currentPrice) {
        BigDecimal threshold = alert.getThresholdValue().setScale(2, RoundingMode.HALF_UP);
        BigDecimal price = currentPrice.setScale(2, RoundingMode.HALF_UP);

        return switch (alert.getConditionType()) {
            case PRICE_ABOVE -> String.format(
                    "%s fiyati %s ₺ esigini gecti, su an %s ₺. Hedef seviyene ulastin.",
                    alert.getAssetSymbol(), threshold, price);
            case PRICE_BELOW -> String.format(
                    "%s fiyati %s ₺ esiginin altina dustu, su an %s ₺. Alim firsati olabilir.",
                    alert.getAssetSymbol(), threshold, price);
            default -> String.format("%s alarmi tetiklendi: %s ₺", alert.getAssetSymbol(), price);
        };
    }
}