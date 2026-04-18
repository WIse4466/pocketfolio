package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AssetHistoryResponse;
import com.pocketfolio.backend.dto.PortfolioSnapshotResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.AssetSnapshotRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetSnapshotService 單元測試")
class AssetSnapshotServiceTest {

    @Mock private AssetSnapshotRepository snapshotRepository;
    @Mock private AssetRepository assetRepository;

    @InjectMocks private AssetSnapshotService service;

    static final UUID CURRENT_USER_ID = UUID.randomUUID();
    static final UUID OTHER_USER_ID   = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        User user = new User();
        user.setId(CURRENT_USER_ID);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User userWith(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Asset assetWith(UUID id, UUID ownerId) {
        Asset a = new Asset();
        a.setId(id);
        a.setSymbol("BTC");
        a.setName("比特幣");
        a.setType(AssetType.CRYPTO);
        a.setQuantity(new BigDecimal("1"));
        a.setCostPrice(new BigDecimal("80000"));
        a.setCurrentPrice(new BigDecimal("100000"));
        a.setUser(userWith(ownerId));
        Account account = new Account();
        account.setId(UUID.randomUUID());
        a.setAccount(account);
        return a;
    }

    private AssetSnapshot snapshotWith(UUID assetId, LocalDate date) {
        AssetSnapshot s = new AssetSnapshot();
        s.setId(UUID.randomUUID());
        Asset asset = new Asset();
        asset.setId(assetId);
        s.setAsset(asset);
        s.setUser(userWith(CURRENT_USER_ID));
        s.setSymbol("BTC");
        s.setAssetName("比特幣");
        s.setAssetType(AssetType.CRYPTO);
        s.setQuantity(new BigDecimal("1"));
        s.setCostPrice(new BigDecimal("80000"));
        s.setCurrentPrice(new BigDecimal("100000"));
        s.setMarketValue(new BigDecimal("100000"));
        s.setProfitLoss(new BigDecimal("20000"));
        s.setProfitLossPercent(new BigDecimal("25.00"));
        s.setSnapshotDate(date);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createSnapshot
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSnapshot（建立單一快照）")
    class CreateSnapshot {

        @Test
        @DisplayName("成功建立快照，回傳 Response")
        void create_happyPath_savesAndReturnsResponse() {
            UUID assetId = UUID.randomUUID();
            Asset asset = assetWith(assetId, CURRENT_USER_ID);
            given(assetRepository.findById(assetId)).willReturn(Optional.of(asset));
            given(snapshotRepository.existsByAssetIdAndSnapshotDate(eq(assetId), any(LocalDate.class)))
                    .willReturn(false);
            AssetSnapshot saved = snapshotWith(assetId, LocalDate.now());
            given(snapshotRepository.save(any())).willReturn(saved);

            var response = service.createSnapshot(assetId);

            then(snapshotRepository).should().save(any());
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("今天已有快照：跳過儲存，回傳 null")
        void create_alreadyExists_returnsNull() {
            UUID assetId = UUID.randomUUID();
            Asset asset = assetWith(assetId, CURRENT_USER_ID);
            given(assetRepository.findById(assetId)).willReturn(Optional.of(asset));
            given(snapshotRepository.existsByAssetIdAndSnapshotDate(eq(assetId), any(LocalDate.class)))
                    .willReturn(true);

            var response = service.createSnapshot(assetId);

            assertThat(response).isNull();
            then(snapshotRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("資產不存在：拋出 IllegalArgumentException")
        void create_assetNotFound_throws() {
            UUID assetId = UUID.randomUUID();
            given(assetRepository.findById(assetId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSnapshot(assetId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("找不到");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createAllSnapshots
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAllSnapshots（批次建立快照）")
    class CreateAllSnapshots {

        @Test
        @DisplayName("跳過已有快照的資產，回傳實際建立數量")
        void createAll_skipsExisting_returnsCount() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Asset a1 = assetWith(id1, CURRENT_USER_ID);
            Asset a2 = assetWith(id2, CURRENT_USER_ID);
            given(assetRepository.findAll()).willReturn(List.of(a1, a2));
            given(snapshotRepository.existsByAssetIdAndSnapshotDate(eq(id1), any(LocalDate.class)))
                    .willReturn(false);
            given(snapshotRepository.existsByAssetIdAndSnapshotDate(eq(id2), any(LocalDate.class)))
                    .willReturn(true); // already has snapshot
            given(snapshotRepository.save(any())).willReturn(snapshotWith(id1, LocalDate.now()));

            int count = service.createAllSnapshots();

            assertThat(count).isEqualTo(1);
            then(snapshotRepository).should(times(1)).save(any());
        }

        @Test
        @DisplayName("無資產時回傳 0")
        void createAll_noAssets_returnsZero() {
            given(assetRepository.findAll()).willReturn(List.of());

            int count = service.createAllSnapshots();

            assertThat(count).isZero();
            then(snapshotRepository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAssetHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAssetHistory（查詢歷史）")
    class GetAssetHistory {

        @Test
        @DisplayName("查詢自己的資產歷史：回傳資料點 List")
        void getHistory_ownedByCurrentUser_returnsDataPoints() {
            UUID assetId = UUID.randomUUID();
            Asset asset = assetWith(assetId, CURRENT_USER_ID);
            given(assetRepository.findById(assetId)).willReturn(Optional.of(asset));

            LocalDate today = LocalDate.now();
            AssetSnapshot snap = snapshotWith(assetId, today.minusDays(1));
            given(snapshotRepository.findByAssetIdAndSnapshotDateBetweenOrderBySnapshotDate(
                    eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(snap));

            AssetHistoryResponse response = service.getAssetHistory(assetId, 7);

            assertThat(response.getSymbol()).isEqualTo("BTC");
            assertThat(response.getHistory()).hasSize(1);
        }

        @Test
        @DisplayName("查詢他人的資產歷史：拋出 IllegalArgumentException")
        void getHistory_ownedByOtherUser_throws() {
            UUID assetId = UUID.randomUUID();
            given(assetRepository.findById(assetId))
                    .willReturn(Optional.of(assetWith(assetId, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.getAssetHistory(assetId, 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");
        }

        @Test
        @DisplayName("days 為 null 時，預設使用 30 天")
        void getHistory_nullDays_defaults30() {
            UUID assetId = UUID.randomUUID();
            given(assetRepository.findById(assetId))
                    .willReturn(Optional.of(assetWith(assetId, CURRENT_USER_ID)));
            given(snapshotRepository.findByAssetIdAndSnapshotDateBetweenOrderBySnapshotDate(
                    any(), any(), any())).willReturn(List.of());

            service.getAssetHistory(assetId, null);

            LocalDate today = LocalDate.now();
            then(snapshotRepository).should().findByAssetIdAndSnapshotDateBetweenOrderBySnapshotDate(
                    assetId, today.minusDays(30), today);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPortfolioSnapshot
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPortfolioSnapshot（投資組合快照）")
    class GetPortfolioSnapshot {

        @Test
        @DisplayName("有快照資料：回傳正確的加總市值與損益")
        void getPortfolio_withSnapshots_returnsAggregatedTotals() {
            LocalDate date = LocalDate.of(2026, 1, 1);
            UUID assetId = UUID.randomUUID();

            AssetSnapshot s1 = snapshotWith(assetId, date);
            s1.setMarketValue(new BigDecimal("100000"));
            s1.setCostPrice(new BigDecimal("80000"));
            s1.setQuantity(new BigDecimal("1"));

            given(snapshotRepository.findByUserIdAndSnapshotDate(CURRENT_USER_ID, date))
                    .willReturn(List.of(s1));

            PortfolioSnapshotResponse response = service.getPortfolioSnapshot(date);

            assertThat(response).isNotNull();
            assertThat(response.getTotalMarketValue()).isEqualByComparingTo("100000");
            assertThat(response.getTotalCost()).isEqualByComparingTo("80000");
            assertThat(response.getTotalProfitLoss()).isEqualByComparingTo("20000");
            assertThat(response.getAssetCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("無快照資料：回傳 null")
        void getPortfolio_noSnapshots_returnsNull() {
            LocalDate date = LocalDate.of(2026, 1, 1);
            given(snapshotRepository.findByUserIdAndSnapshotDate(CURRENT_USER_ID, date))
                    .willReturn(List.of());

            PortfolioSnapshotResponse response = service.getPortfolioSnapshot(date);

            assertThat(response).isNull();
        }
    }
}
