package com.pocketfolio.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pocketfolio.backend.entity.KnownAsset;
import com.pocketfolio.backend.repository.KnownAssetRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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

    // 網路或 API 例外時自動重試（2s → 4s → 8s）；sanity check 回傳 0 不觸發 retry
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Transactional
    public int syncTwse() {
        log.info("開始同步 TWSE 上市股票與 ETF...");
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
    }

    // ────────────── TPEX 上櫃（含 ETF） ──────────────

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Transactional
    public int syncTpex() {
        log.info("開始同步 TPEX 上櫃股票與 ETF...");
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
    }

    // ────────────── CoinGecko 加密貨幣 ──────────────

    /**
     * 從 /coins/markets 抓前 1000 名的市值排名（每頁 250 筆，共 4 頁）。
     * 免費 API rate limit 約 30 req/min，4 頁之間各 sleep 2s。
     */
    private java.util.Map<String, Integer> fetchMarketCapRanks() {
        java.util.Map<String, Integer> rankMap = new java.util.HashMap<>();
        WebClient client = webClientBuilder.baseUrl(coinGeckoBaseUrl).build();
        for (int page = 1; page <= 4; page++) {
            try {
                int p = page;
                List<CoinGeckoMarketItem> pageItems = client.get()
                        .uri(u -> u.path("/coins/markets")
                                .queryParam("vs_currency", "usd")
                                .queryParam("order", "market_cap_desc")
                                .queryParam("per_page", 250)
                                .queryParam("page", p)
                                .build())
                        .retrieve()
                        .bodyToFlux(CoinGeckoMarketItem.class)
                        .collectList()
                        .block();
                if (pageItems != null) {
                    pageItems.forEach(item -> {
                        if (item.getId() != null && item.getMarketCapRank() != null) {
                            rankMap.put(item.getId(), item.getMarketCapRank());
                        }
                    });
                }
                if (page < 4) Thread.sleep(2000); // 避免 rate limit
            } catch (Exception e) {
                log.warn("fetchMarketCapRanks page {} 失敗，繼續其餘頁: {}", page, e.getMessage());
            }
        }
        log.info("市值排名同步完成：{} 筆", rankMap.size());
        return rankMap;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Transactional
    public int syncCrypto() {
        log.info("開始同步 CoinGecko 加密貨幣清單...");

        // 先取得前 1000 名市值排名
        java.util.Map<String, Integer> rankMap = fetchMarketCapRanks();

        // CoinGecko /coins/list 回應約 1MB，超過 WebClient 預設 256KB buffer，需明確指定上限
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB
                .build();
        List<CoinGeckoListItem> items = webClientBuilder.baseUrl(coinGeckoBaseUrl)
                .exchangeStrategies(strategies)
                .build()
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
            ka.setMarketCapRank(rankMap.get(item.getId()));     // 前 1000 名有值，其餘 null
            toSave.add(ka);
        }

        knownAssetRepository.deleteByAssetType("CRYPTO");
        knownAssetRepository.saveAll(toSave);
        log.info("CoinGecko 同步完成：{} 筆", toSave.size());
        return toSave.size();
    }

    // ────────────── 全部同步 ──────────────

    public void syncAll() {
        int twse = syncSafe("TWSE", this::syncTwse);
        int tpex = syncSafe("TPEX", this::syncTpex);
        int crypto = syncSafe("CoinGecko", this::syncCrypto);
        log.info("資產清單全量同步完成 — TWSE: {}, TPEX: {}, Crypto: {}", twse, tpex, crypto);
    }

    // 包裝每個 sync 方法：3 次 retry 都失敗後記錄 error，不讓例外往上傳讓 scheduler 中斷
    private int syncSafe(String source, java.util.function.IntSupplier syncFn) {
        try {
            return syncFn.getAsInt();
        } catch (Exception e) {
            log.error("{} 同步最終失敗（已重試 3 次）：{}", source, e.getMessage());
            return 0;
        }
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CoinGeckoMarketItem {
        private String id;
        @JsonProperty("market_cap_rank")
        private Integer marketCapRank;
    }
}
