package com.pocketfolio.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "known_assets", indexes = {
        @Index(name = "idx_known_assets_symbol", columnList = "symbol"),
        @Index(name = "idx_known_assets_asset_type", columnList = "asset_type")
})
@Getter
@Setter
public class KnownAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 資產類型：STOCK / CRYPTO
    @Column(name = "asset_type", nullable = false, length = 10)
    private String assetType;

    // Yahoo Finance / CoinGecko API 相容代號
    // 台股上市：2330.TW，上櫃：6547.TWO，加密：bitcoin（CoinGecko id）
    @Column(nullable = false, unique = true, length = 100)
    private String symbol;

    // 顯示名稱：台積電、Bitcoin
    @Column(nullable = false, length = 200)
    private String name;

    // 用戶看到的短代碼：2330、BTC（搜尋用）
    @Column(name = "display_code", nullable = false, length = 50)
    private String displayCode;
}
