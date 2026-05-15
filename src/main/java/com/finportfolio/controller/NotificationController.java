package com.finportfolio.controller;

import com.finportfolio.dto.AlertRequest;
import com.finportfolio.dto.NotificationResponse;
import com.finportfolio.entity.PriceAlert;
import com.finportfolio.exception.BusinessException;
import com.finportfolio.security.JwtService;
import com.finportfolio.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "6. Bildirimler", description = "Fiyat alarmları ve bildirim yönetimi")
public class NotificationController {

    private final AlertService alertService;
    private final JwtService jwtService;

    // ============ ALARMLAR ============

    @PostMapping("/alerts")
    public ResponseEntity<PriceAlert> createAlert(
            @Valid @RequestBody AlertRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.createAlert(userId, request));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<PriceAlert>> getMyAlerts(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(alertService.getUserAlerts(userId));
    }

    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long alertId, HttpServletRequest request) {
        Long userId = extractUserId(request);
        alertService.deleteAlert(userId, alertId);
        return ResponseEntity.noContent().build();
    }

    // ============ BİLDİRİMLER ============

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(alertService.getUserNotifications(userId));
    }

    @GetMapping("/notifications/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(alertService.getUnreadNotifications(userId));
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(Map.of("unread", alertService.countUnread(userId)));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = extractUserId(request);
        alertService.markAsRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = extractUserId(request);
        alertService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtService.extractUserId(authHeader.substring(7));
        }
        throw new BusinessException("Authorization header eksik", HttpStatus.UNAUTHORIZED);
    }
}