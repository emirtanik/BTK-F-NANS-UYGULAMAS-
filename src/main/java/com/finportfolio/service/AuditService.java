package com.finportfolio.service;

import com.finportfolio.entity.AuditLog;
import com.finportfolio.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Audit log kaydeder. Async cunku ana islemi yavasletmesin.
     */
    @Async
    public void log(Long userId, String action, boolean success, String details, HttpServletRequest request) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .success(success)
                    .details(details != null && details.length() > 1000 ? details.substring(0, 1000) : details)
                    .ipAddress(extractIp(request))
                    .userAgent(extractUserAgent(request))
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Audit log yazilamadi: {}", e.getMessage());
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 500) {
            return ua.substring(0, 500);
        }
        return ua;
    }
}