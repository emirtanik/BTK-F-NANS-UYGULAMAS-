package com.finportfolio.repository;

import com.finportfolio.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findAllByUserId(Long userId);

    List<PriceAlert> findAllByIsActiveTrueAndIsTriggeredFalse();
}