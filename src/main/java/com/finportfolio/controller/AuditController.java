package com.finportfolio.controller;

import com.finportfolio.entity.AuditLog;
import com.finportfolio.exception.BusinessException;
import com.finportfolio.repository.AuditLogRepository;
import com.finportfolio.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@io.swagger.v3.oas.annotations.tags.Tag(name = "7. Audit Log", description = "Güvenlik ve aktivite kayıtları")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final JwtService jwtService;

    public AuditController(AuditLogRepository auditLogRepository, JwtService jwtService) {
        this.auditLogRepository = auditLogRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/my-logs")
    public ResponseEntity<List<AuditLog>> getMyLogs(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(auditLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecent(HttpServletRequest request) {
        extractUserId(request);
        return ResponseEntity.ok(auditLogRepository.findTop100ByOrderByCreatedAtDesc());
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtService.extractUserId(authHeader.substring(7));
        }
        throw new BusinessException("Authorization header eksik", HttpStatus.UNAUTHORIZED);
    }
}