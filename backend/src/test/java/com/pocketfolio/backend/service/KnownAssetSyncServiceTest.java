package com.pocketfolio.backend.service;

import com.pocketfolio.backend.repository.KnownAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class KnownAssetSyncServiceTest {

    @Mock private KnownAssetRepository knownAssetRepository;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    // raw type 避免 Mockito wildcard 型別推斷失敗
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private KnownAssetSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new KnownAssetSyncService(knownAssetRepository, webClientBuilder);
        ReflectionTestUtils.setField(syncService, "coinGeckoBaseUrl", "https://api.coingecko.com/api/v3");

        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.exchangeStrategies(any(org.springframework.web.reactive.function.client.ExchangeStrategies.class)))
                .thenReturn(webClientBuilder);

        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    // ────────────── TWSE Sanity Check ──────────────

    @Nested
    @DisplayName("syncTwse()")
    class SyncTwse {

        @Test
        @DisplayName("API 回傳筆數低於閾值（500）時，不清除舊資料")
        void sanityCheck_skipsWhenTooFewItems() {
            given(responseSpec.bodyToFlux(KnownAssetSyncService.TwseItem.class))
                    .willReturn(Flux.just(twseItem("2330"), twseItem("2317"), twseItem("2454")));

            int result = syncService.syncTwse();

            assertThat(result).isEqualTo(0);
            verify(knownAssetRepository, never()).deleteByAssetType("STOCK_TW");
            verify(knownAssetRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("API 回傳空串列時，不清除舊資料")
        void sanityCheck_skipsWhenEmpty() {
            given(responseSpec.bodyToFlux(KnownAssetSyncService.TwseItem.class))
                    .willReturn(Flux.empty());

            int result = syncService.syncTwse();

            assertThat(result).isEqualTo(0);
            verify(knownAssetRepository, never()).deleteByAssetType("STOCK_TW");
        }
    }

    // ────────────── TPEX Sanity Check ──────────────

    @Nested
    @DisplayName("syncTpex()")
    class SyncTpex {

        @Test
        @DisplayName("API 回傳筆數低於閾值（400）時，不清除舊資料")
        void sanityCheck_skipsWhenTooFewItems() {
            given(responseSpec.bodyToFlux(KnownAssetSyncService.TpexItem.class))
                    .willReturn(Flux.just(tpexItem("6547"), tpexItem("3008")));

            int result = syncService.syncTpex();

            assertThat(result).isEqualTo(0);
            verify(knownAssetRepository, never()).deleteByAssetType("STOCK_TWO");
            verify(knownAssetRepository, never()).saveAll(any());
        }
    }

    // ────────────── Helper methods ──────────────

    private KnownAssetSyncService.TwseItem twseItem(String code) {
        KnownAssetSyncService.TwseItem item = new KnownAssetSyncService.TwseItem();
        item.setCode(code);
        item.setName("公司" + code);
        return item;
    }

    private KnownAssetSyncService.TpexItem tpexItem(String code) {
        KnownAssetSyncService.TpexItem item = new KnownAssetSyncService.TpexItem();
        item.setCode(code);
        item.setName("公司" + code);
        return item;
    }
}
