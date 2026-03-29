package com.pocketfolio.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pocketfolio.backend.entity.KnownAsset;
import com.pocketfolio.backend.repository.KnownAssetRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnownAssetSyncService {

    private final KnownAssetRepository knownAssetRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${api.coingecko.base-url}")
    private String coinGeckoBaseUrl;

    private static final String TWSE_URL =
            "https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL";
    private static final String TPEX_URL =
            "https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes";

    // 最低合理筆數（低於此值視為 API 異常，拒絕寫入）
    private static final int TWSE_MIN_COUNT = 500;
    private static final int TPEX_MIN_COUNT = 400;
    private static final int CRYPTO_MIN_COUNT = 5000;

    // ────────────── TWSE 上市（含 ETF） ──────────────

    @Transactional
    public int syncTwse() {
        log.info("開始同步 TWSE 上市股票與 ETF...");
        try {
            List<TwseItem> items = webClientBuilder.build()
                    .get().uri(TWSE_URL)
                    .retrieve()
                    .bodyToFlux(TwseItem.class)
                    .collectList()
                    .block();

            if (items == null || items.size() < TWSE_MIN_COUNT) {
                log.warn("TWSE 回傳資料量異常（{}筆），跳過本次同步", items == null ? 0 : items.size());
                return 0;
            }

            List<KnownAsset> toSave = new ArrayList<>();
            for (TwseItem item : items) {
                if (item.getCode() == null || item.getName() == null) continue;
                KnownAsset ka = new KnownAsset();
                ka.setAssetType("STOCK_TW");
                ka.setSymbol(item.getCode() + ".TW");
                ka.setDisplayCode(item.getCode());
                ka.setName(item.getName());
                toSave.add(ka);
            }

            // delete + saveAll 在同一個 @Transactional，任一失敗則全部 rollback
            knownAssetRepository.deleteByAssetType("STOCK_TW");
            knownAssetRepository.saveAll(toSave);
            log.info("TWSE 同步完成：{} 筆", toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("TWSE 同步失敗：{}", e.getMessage(), e);
            return 0;
        }
    }

    // ────────────── TPEX 上櫃（含 ETF） ──────────────

    @Transactional
    public int syncTpex() {
        log.info("開始同步 TPEX 上櫃股票與 ETF...");
        try {
            List<TpexItem> items = webClientBuilder.build()
                    .get().uri(TPEX_URL)
                    .retrieve()
                    .bodyToFlux(TpexItem.class)
                    .collectList()
                    .block();

            if (items == null || items.size() < TPEX_MIN_COUNT) {
                log.warn("TPEX 回傳資料量異常（{}筆），跳過本次同步", items == null ? 0 : items.size());
                return 0;
            }

            List<KnownAsset> toSave = new ArrayList<>();
            for (TpexItem item : items) {
                if (item.getCode() == null || item.getName() == null) continue;
                KnownAsset ka = new KnownAsset();
                ka.setAssetType("STOCK_TWO");
                ka.setSymbol(item.getCode() + ".TWO");
                ka.setDisplayCode(item.getCode());
                ka.setName(item.getName());
                toSave.add(ka);
            }

            knownAssetRepository.deleteByAssetType("STOCK_TWO");
            knownAssetRepository.saveAll(toSave);
            log.info("TPEX 同步完成：{} 筆", toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("TPEX 同步失敗：{}", e.getMessage(), e);
            return 0;
        }
    }

    // ────────────── CoinGecko 加密貨幣 ──────────────

    @Transactional
    public int syncCrypto() {
        log.info("開始同步 CoinGecko 加密貨幣清單...");
        try {
            List<CoinGeckoListItem> items = webClientBuilder.baseUrl(coinGeckoBaseUrl).build()
                    .get().uri("/coins/list")
                    .retrieve()
                    .bodyToFlux(CoinGeckoListItem.class)
                    .collectList()
                    .block();

            if (items == null || items.size() < CRYPTO_MIN_COUNT) {
                log.warn("CoinGecko 回傳資料量異常（{}筆），跳過本次同步", items == null ? 0 : items.size());
                return 0;
            }

            List<KnownAsset> toSave = new ArrayList<>();
            for (CoinGeckoListItem item : items) {
                if (item.getId() == null || item.getSymbol() == null || item.getName() == null) continue;
                KnownAsset ka = new KnownAsset();
                ka.setAssetType("CRYPTO");
                ka.setSymbol(item.getId());                         // CoinGecko id，例如 "bitcoin"
                ka.setDisplayCode(item.getSymbol().toUpperCase());  // 例如 "BTC"
                ka.setName(item.getName());                         // 例如 "Bitcoin"
                toSave.add(ka);
            }

            knownAssetRepository.deleteByAssetType("CRYPTO");
            knownAssetRepository.saveAll(toSave);
            log.info("CoinGecko 同步完成：{} 筆", toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("CoinGecko 同步失敗：{}", e.getMessage(), e);
            return 0;
        }
    }

    // ────────────── 全部同步 ──────────────

    public void syncAll() {
        int twse = syncTwse();
        int tpex = syncTpex();
        int crypto = syncCrypto();
        log.info("資產清單全量同步完成 — TWSE: {}, TPEX: {}, Crypto: {}", twse, tpex, crypto);
    }

    // ────────────── 內部 DTO ──────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TwseItem {
        @JsonProperty("Code")
        private String code;
        @JsonProperty("Name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TpexItem {
        @JsonProperty("SecuritiesCompanyCode")
        private String code;
        @JsonProperty("CompanyName")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CoinGeckoListItem {
        private String id;
        private String symbol;
        private String name;
    }
}
