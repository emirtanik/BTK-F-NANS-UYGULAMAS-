package com.finportfolio.repository;

import com.finportfolio.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    List<PriceSnapshot> findAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM PriceSnapshot p WHERE p.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}