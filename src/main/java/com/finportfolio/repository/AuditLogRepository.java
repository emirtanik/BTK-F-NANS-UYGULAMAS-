package com.finportfolio.repository;

import com.finportfolio.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}