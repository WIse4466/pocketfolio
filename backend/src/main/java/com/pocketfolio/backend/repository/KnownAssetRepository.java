package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.KnownAsset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnownAssetRepository extends JpaRepository<KnownAsset, UUID> {

    boolean existsBySymbol(String symbol);

    // 搜尋名稱或顯示代碼，市值排名小的優先（垃圾幣排後面），無排名的排最後
    @Query("""
            SELECT k FROM KnownAsset k
            WHERE k.assetType = :assetType
              AND (LOWER(k.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(k.displayCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY k.marketCapRank ASC NULLS LAST, k.displayCode ASC
            """)
    List<KnownAsset> searchByKeyword(@Param("assetType") String assetType,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);

    long countByAssetType(String assetType);

    void deleteByAssetType(String assetType);
}
