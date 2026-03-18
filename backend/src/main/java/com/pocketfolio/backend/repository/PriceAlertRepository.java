package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {

    // 查詢用戶的所有警報
    List<PriceAlert> findByUserId(UUID userId);

    // 查詢用戶的啟用警報
    List<PriceAlert> findByUserIdAndActiveTrue(UUID userId);

    // 查詢特定資產的所有啟用警報
    @Query("SELECT a FROM PriceAlert a WHERE a.symbol = :symbol " +
            "AND a.active = true AND a.triggered = false")
    List<PriceAlert> findActiveAlertsBySymbol(@Param("symbol") String symbol);
}