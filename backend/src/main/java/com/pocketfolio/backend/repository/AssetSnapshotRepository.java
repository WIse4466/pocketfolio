package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.AssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, UUID> {

    // 查詢某資產的所有快照（依日期排序）
    List<AssetSnapshot> findByAssetIdOrderBySnapshotDateDesc(UUID assetId);

    // 查詢某資產在日期範圍內的快照
    List<AssetSnapshot> findByAssetIdAndSnapshotDateBetweenOrderBySnapshotDate(
            UUID assetId, LocalDate startDate, LocalDate endDate);

    // 查詢某用戶在特定日期的所有快照
    List<AssetSnapshot> findByUserIdAndSnapshotDate(UUID userId, LocalDate date);

    // 查詢某用戶在日期範圍內的所有快照
    List<AssetSnapshot> findByUserIdAndSnapshotDateBetweenOrderBySnapshotDate(
            UUID userId, LocalDate startDate, LocalDate endDate);

    // 檢查某資產在特定日期是否已有快照
    boolean existsByAssetIdAndSnapshotDate(UUID assetId, LocalDate date);

    // 查詢某資產最新的快照
    Optional<AssetSnapshot> findFirstByAssetIdOrderBySnapshotDateDesc(UUID assetId);

    // 統計：查詢某用戶在特定日期的總資產市值
    @Query("SELECT SUM(s.marketValue) FROM AssetSnapshot s " +
            "WHERE s.user.id = :userId AND s.snapshotDate = :date")
    BigDecimal calculateTotalMarketValueByUserAndDate(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date);
}