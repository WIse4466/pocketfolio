package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.PriceAlertRequest;
import com.pocketfolio.backend.dto.PriceAlertResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.PriceAlertRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAlertService 單元測試")
class PriceAlertServiceTest {

    @Mock private PriceAlertRepository alertRepository;
    @Mock private AssetRepository assetRepository;

    @InjectMocks private PriceAlertService service;

    @Captor private ArgumentCaptor<PriceAlert> alertCaptor;

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

    private PriceAlert alertWith(UUID id, UUID ownerId) {
        PriceAlert a = new PriceAlert();
        a.setId(id);
        a.setSymbol("BTC");
        a.setAssetType(AssetType.CRYPTO);
        a.setCondition(PriceAlert.AlertCondition.ABOVE);
        a.setTargetPrice(new BigDecimal("100000"));
        a.setActive(true);
        a.setTriggered(false);
        a.setUser(userWith(ownerId));
        return a;
    }

    private PriceAlertRequest alertRequest() {
        PriceAlertRequest req = new PriceAlertRequest();
        req.setSymbol("btc");
        req.setAssetType(AssetType.CRYPTO);
        req.setCondition(PriceAlert.AlertCondition.ABOVE);
        req.setTargetPrice(new BigDecimal("100000"));
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createAlert
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAlert（建立警報）")
    class CreateAlert {

        @Test
        @DisplayName("成功建立警報：symbol 轉大寫，active=true，triggered=false")
        void create_happyPath_savesAlertWithCorrectDefaults() {
            PriceAlert saved = alertWith(UUID.randomUUID(), CURRENT_USER_ID);
            given(alertRepository.save(any())).willReturn(saved);

            service.createAlert(alertRequest());

            then(alertRepository).should().save(alertCaptor.capture());
            PriceAlert captured = alertCaptor.getValue();
            assertThat(captured.getSymbol()).isEqualTo("BTC");
            assertThat(captured.isActive()).isTrue();
            assertThat(captured.isTriggered()).isFalse();
            assertThat(captured.getUser().getId()).isEqualTo(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("指定自己的資產：成功綁定 Asset")
        void create_withOwnAsset_linksAsset() {
            UUID assetId = UUID.randomUUID();
            Asset asset = new Asset();
            asset.setId(assetId);
            asset.setUser(userWith(CURRENT_USER_ID));

            PriceAlertRequest req = alertRequest();
            req.setAssetId(assetId);

            PriceAlert saved = alertWith(UUID.randomUUID(), CURRENT_USER_ID);
            saved.setAsset(asset);
            given(assetRepository.findById(assetId)).willReturn(Optional.of(asset));
            given(alertRepository.save(any())).willReturn(saved);

            service.createAlert(req);

            then(alertRepository).should().save(alertCaptor.capture());
            assertThat(alertCaptor.getValue().getAsset()).isEqualTo(asset);
        }

        @Test
        @DisplayName("指定他人的資產：拋出 IllegalArgumentException 且不儲存")
        void create_withOtherUserAsset_throws() {
            UUID assetId = UUID.randomUUID();
            Asset asset = new Asset();
            asset.setId(assetId);
            asset.setUser(userWith(OTHER_USER_ID));

            PriceAlertRequest req = alertRequest();
            req.setAssetId(assetId);

            given(assetRepository.findById(assetId)).willReturn(Optional.of(asset));

            assertThatThrownBy(() -> service.createAlert(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");

            then(alertRepository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAlert
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAlert（查詢單筆）")
    class GetAlert {

        @Test
        @DisplayName("查詢自己的警報，回傳正確 Response")
        void getOne_ownedByCurrentUser_returnsResponse() {
            UUID id = UUID.randomUUID();
            given(alertRepository.findById(id)).willReturn(Optional.of(alertWith(id, CURRENT_USER_ID)));

            PriceAlertResponse response = service.getAlert(id);

            assertThat(response.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("查詢他人的警報：拋出 ResourceNotFoundException")
        void getOne_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(alertRepository.findById(id)).willReturn(Optional.of(alertWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.getAlert(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("查詢不存在的警報：拋出 ResourceNotFoundException")
        void getOne_notFound_throws() {
            UUID id = UUID.randomUUID();
            given(alertRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAlert(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserAlerts / getUserActiveAlerts
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserAlerts / getUserActiveAlerts（查詢列表）")
    class GetUserAlerts {

        @Test
        @DisplayName("getUserAlerts：只查詢當前用戶的警報")
        void getUserAlerts_queriesCurrentUserOnly() {
            given(alertRepository.findByUserId(CURRENT_USER_ID)).willReturn(List.of());

            service.getUserAlerts();

            then(alertRepository).should().findByUserId(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("getUserActiveAlerts：只查詢當前用戶的啟用警報")
        void getUserActiveAlerts_queriesActiveOnly() {
            given(alertRepository.findByUserIdAndActiveTrue(CURRENT_USER_ID)).willReturn(List.of());

            service.getUserActiveAlerts();

            then(alertRepository).should().findByUserIdAndActiveTrue(CURRENT_USER_ID);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAlert
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAlert（更新）")
    class UpdateAlert {

        @Test
        @DisplayName("更新警報：欄位更新，觸發狀態重置")
        void update_happyPath_resetsTriggeredState() {
            UUID id = UUID.randomUUID();
            PriceAlert existing = alertWith(id, CURRENT_USER_ID);
            existing.setTriggered(true);
            given(alertRepository.findById(id)).willReturn(Optional.of(existing));
            given(alertRepository.save(any())).willReturn(existing);

            PriceAlertRequest req = alertRequest();
            req.setTargetPrice(new BigDecimal("200000"));

            service.updateAlert(id, req);

            then(alertRepository).should().save(alertCaptor.capture());
            PriceAlert captured = alertCaptor.getValue();
            assertThat(captured.getTargetPrice()).isEqualByComparingTo("200000");
            assertThat(captured.isTriggered()).isFalse();
            assertThat(captured.getTriggeredAt()).isNull();
        }

        @Test
        @DisplayName("更新他人的警報：拋出 ResourceNotFoundException 且不儲存")
        void update_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(alertRepository.findById(id)).willReturn(Optional.of(alertWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.updateAlert(id, alertRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(alertRepository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAlert
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAlert（刪除）")
    class DeleteAlert {

        @Test
        @DisplayName("成功刪除自己的警報")
        void delete_happyPath_deletesById() {
            UUID id = UUID.randomUUID();
            given(alertRepository.findById(id)).willReturn(Optional.of(alertWith(id, CURRENT_USER_ID)));

            service.deleteAlert(id);

            then(alertRepository).should().deleteById(id);
        }

        @Test
        @DisplayName("刪除他人的警報：拋出 ResourceNotFoundException 且不執行刪除")
        void delete_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(alertRepository.findById(id)).willReturn(Optional.of(alertWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.deleteAlert(id))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(alertRepository).should(never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toggleAlert
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleAlert（啟用/停用）")
    class ToggleAlert {

        @Test
        @DisplayName("重新啟用警報：同時重置觸發狀態")
        void toggle_enable_resetsTriggeredState() {
            UUID id = UUID.randomUUID();
            PriceAlert alert = alertWith(id, CURRENT_USER_ID);
            alert.setActive(false);
            alert.setTriggered(true);
            given(alertRepository.findById(id)).willReturn(Optional.of(alert));
            given(alertRepository.save(any())).willReturn(alert);

            service.toggleAlert(id, true);

            then(alertRepository).should().save(alertCaptor.capture());
            PriceAlert captured = alertCaptor.getValue();
            assertThat(captured.isActive()).isTrue();
            assertThat(captured.isTriggered()).isFalse();
        }

        @Test
        @DisplayName("停用警報：只設定 active=false，不重置觸發狀態")
        void toggle_disable_onlySetsActiveFalse() {
            UUID id = UUID.randomUUID();
            PriceAlert alert = alertWith(id, CURRENT_USER_ID);
            alert.setTriggered(true);
            given(alertRepository.findById(id)).willReturn(Optional.of(alert));
            given(alertRepository.save(any())).willReturn(alert);

            service.toggleAlert(id, false);

            then(alertRepository).should().save(alertCaptor.capture());
            PriceAlert captured = alertCaptor.getValue();
            assertThat(captured.isActive()).isFalse();
            assertThat(captured.isTriggered()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkPriceAlerts
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkPriceAlerts（價格觸發邏輯）")
    class CheckPriceAlerts {

        @Test
        @DisplayName("ABOVE 條件：當前價格 >= 目標價格 → 觸發並標記")
        void check_aboveCondition_triggersWhenPriceReachesTarget() {
            PriceAlert alert = alertWith(UUID.randomUUID(), CURRENT_USER_ID);
            alert.setCondition(PriceAlert.AlertCondition.ABOVE);
            alert.setTargetPrice(new BigDecimal("100000"));
            given(alertRepository.findActiveAlertsBySymbol("BTC")).willReturn(List.of(alert));
            given(alertRepository.save(any())).willReturn(alert);

            List<PriceAlert> triggered = service.checkPriceAlerts("BTC", new BigDecimal("100001"));

            assertThat(triggered).hasSize(1);
            then(alertRepository).should().save(alertCaptor.capture());
            assertThat(alertCaptor.getValue().isTriggered()).isTrue();
            assertThat(alertCaptor.getValue().getTriggeredAt()).isNotNull();
        }

        @Test
        @DisplayName("ABOVE 條件：當前價格 < 目標價格 → 不觸發")
        void check_aboveCondition_noTriggerWhenPriceBelow() {
            PriceAlert alert = alertWith(UUID.randomUUID(), CURRENT_USER_ID);
            alert.setCondition(PriceAlert.AlertCondition.ABOVE);
            alert.setTargetPrice(new BigDecimal("100000"));
            given(alertRepository.findActiveAlertsBySymbol("BTC")).willReturn(List.of(alert));

            List<PriceAlert> triggered = service.checkPriceAlerts("BTC", new BigDecimal("99999"));

            assertThat(triggered).isEmpty();
            then(alertRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("BELOW 條件：當前價格 <= 目標價格 → 觸發並標記")
        void check_belowCondition_triggersWhenPriceDropsToTarget() {
            PriceAlert alert = alertWith(UUID.randomUUID(), CURRENT_USER_ID);
            alert.setCondition(PriceAlert.AlertCondition.BELOW);
            alert.setTargetPrice(new BigDecimal("50000"));
            given(alertRepository.findActiveAlertsBySymbol("BTC")).willReturn(List.of(alert));
            given(alertRepository.save(any())).willReturn(alert);

            List<PriceAlert> triggered = service.checkPriceAlerts("BTC", new BigDecimal("49999"));

            assertThat(triggered).hasSize(1);
            then(alertRepository).should().save(any());
        }

        @Test
        @DisplayName("BELOW 條件：當前價格 > 目標價格 → 不觸發")
        void check_belowCondition_noTriggerWhenPriceAbove() {
            PriceAlert alert = alertWith(UUID.randomUUID(), CURRENT_USER_ID);
            alert.setCondition(PriceAlert.AlertCondition.BELOW);
            alert.setTargetPrice(new BigDecimal("50000"));
            given(alertRepository.findActiveAlertsBySymbol("BTC")).willReturn(List.of(alert));

            List<PriceAlert> triggered = service.checkPriceAlerts("BTC", new BigDecimal("50001"));

            assertThat(triggered).isEmpty();
        }

        @Test
        @DisplayName("無啟用警報時，回傳空 List 且不呼叫 save")
        void check_noActiveAlerts_returnsEmptyList() {
            given(alertRepository.findActiveAlertsBySymbol("ETH")).willReturn(List.of());

            List<PriceAlert> triggered = service.checkPriceAlerts("ETH", new BigDecimal("3000"));

            assertThat(triggered).isEmpty();
            then(alertRepository).should(never()).save(any());
        }
    }
}
