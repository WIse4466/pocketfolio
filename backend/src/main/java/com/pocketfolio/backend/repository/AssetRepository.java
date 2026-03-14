package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Asset;
import com.pocketfolio.backend.entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    // 查詢某用戶的所有資產
    List<Asset> findByUserId(UUID userId);

    // 查詢某帳戶的所有資產
    List<Asset> findByUserIdAndAccountId(UUID userId, UUID accountId);

    // 查詢某帳戶的特定類型資產
    List<Asset> findByUserIdAndAccountIdAndType(UUID userId, UUID accountId, AssetType type);

    // 查詢某代號的資產（用於檢查重複）
    boolean existsByUserIdAndAccountIdAndSymbol(UUID userId, UUID accountId, String symbol);
}