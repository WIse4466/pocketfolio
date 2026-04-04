package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AssetRequest;
import com.pocketfolio.backend.dto.AssetResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetService 單元測試")
class AssetServiceTest {

    @Mock AssetRepository assetRepository;
    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;

    @InjectMocks AssetService service;

    @Captor ArgumentCaptor<Transaction> transactionCaptor;

    UUID userId;
    UUID investAccountId;
    Account investAccount;
    Asset savedAsset;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        investAccountId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        investAccount = new Account();
        investAccount.setId(investAccountId);
        investAccount.setName("投資帳戶");
        investAccount.setUser(user);

        savedAsset = new Asset();
        savedAsset.setId(UUID.randomUUID());
        savedAsset.setAccount(investAccount);
        savedAsset.setSymbol("2330.TW");
        savedAsset.setName("台積電");
        savedAsset.setType(AssetType.STOCK);
        savedAsset.setQuantity(new BigDecimal("10"));
        savedAsset.setCostPrice(new BigDecimal("800"));
        savedAsset.setCurrentPrice(new BigDecimal("800"));

        // 設定 SecurityContext（模擬已登入用戶）
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AssetRequest buildRequest(UUID fromAccountId) {
        AssetRequest req = new AssetRequest();
        req.setAccountId(investAccountId);
        req.setType(AssetType.STOCK);
        req.setSymbol("2330.TW");
        req.setName("台積電");
        req.setQuantity(new BigDecimal("10"));
        req.setCostPrice(new BigDecimal("800"));
        req.setFromAccountId(fromAccountId);
        return req;
    }

    // ─── 不填來源帳戶 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAsset — 無來源帳戶（fromAccountId = null）")
    class WithoutFromAccount {

        @Test
        @DisplayName("只存資產，完全不建立交易記錄")
        void create_withoutFromAccount_noTransactions() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            AssetResponse response = service.createAsset(buildRequest(null));

            assertThat(response.getSymbol()).isEqualTo("2330.TW");
            then(transactionRepository).should(never()).save(any());
        }
    }

    // ─── 填來源帳戶 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAsset — 有來源帳戶（fromAccountId 填寫）")
    class WithFromAccount {

        UUID fromAccountId;
        Account fromAccount;

        @BeforeEach
        void setUp() {
            fromAccountId = UUID.randomUUID();

            User user = new User();
            user.setId(userId);

            fromAccount = new Account();
            fromAccount.setId(fromAccountId);
            fromAccount.setName("銀行帳戶");
            fromAccount.setUser(user);
        }

        @Test
        @DisplayName("建立一筆 TRANSFER_OUT 和一筆 TRANSFER_IN")
        void create_withFromAccount_createsTwoTransactions() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(accountRepository.findById(fromAccountId)).willReturn(Optional.of(fromAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            service.createAsset(buildRequest(fromAccountId));

            then(transactionRepository).should(times(2)).save(transactionCaptor.capture());
            List<Transaction> saved = transactionCaptor.getAllValues();

            assertThat(saved)
                    .extracting(Transaction::getType)
                    .containsExactlyInAnyOrder(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
        }

        @Test
        @DisplayName("金額 = 數量 × 成本價（10 × 800 = 8000），兩筆相同")
        void create_withFromAccount_correctAmount() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(accountRepository.findById(fromAccountId)).willReturn(Optional.of(fromAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            service.createAsset(buildRequest(fromAccountId));

            then(transactionRepository).should(times(2)).save(transactionCaptor.capture());
            for (Transaction tx : transactionCaptor.getAllValues()) {
                assertThat(tx.getAmount()).isEqualByComparingTo("8000");
            }
        }

        @Test
        @DisplayName("TRANSFER_OUT 綁定來源帳戶，TRANSFER_IN 綁定投資帳戶")
        void create_withFromAccount_correctAccountAssignment() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(accountRepository.findById(fromAccountId)).willReturn(Optional.of(fromAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            service.createAsset(buildRequest(fromAccountId));

            then(transactionRepository).should(times(2)).save(transactionCaptor.capture());
            List<Transaction> saved = transactionCaptor.getAllValues();

            Transaction out = saved.stream()
                    .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                    .findFirst().orElseThrow();
            Transaction in = saved.stream()
                    .filter(t -> t.getType() == TransactionType.TRANSFER_IN)
                    .findFirst().orElseThrow();

            assertThat(out.getAccount().getId()).isEqualTo(fromAccountId);
            assertThat(in.getAccount().getId()).isEqualTo(investAccountId);
        }

        @Test
        @DisplayName("兩筆交易共用同一個 transferGroupId")
        void create_withFromAccount_sharedTransferGroupId() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(accountRepository.findById(fromAccountId)).willReturn(Optional.of(fromAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            service.createAsset(buildRequest(fromAccountId));

            then(transactionRepository).should(times(2)).save(transactionCaptor.capture());
            List<Transaction> saved = transactionCaptor.getAllValues();

            UUID groupId = saved.get(0).getTransferGroupId();
            assertThat(groupId).isNotNull();
            assertThat(saved.get(1).getTransferGroupId()).isEqualTo(groupId);
        }

        @Test
        @DisplayName("note 包含資產名稱「台積電」")
        void create_withFromAccount_noteContainsAssetName() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(accountRepository.findById(fromAccountId)).willReturn(Optional.of(fromAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            service.createAsset(buildRequest(fromAccountId));

            then(transactionRepository).should(times(2)).save(transactionCaptor.capture());
            for (Transaction tx : transactionCaptor.getAllValues()) {
                assertThat(tx.getNote()).contains("台積電");
            }
        }

        @Test
        @DisplayName("fromAccountId 與 accountId 相同，拋出 IllegalArgumentException，不存任何交易")
        void create_sameFromAndToAccount_throws() {
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            AssetRequest req = buildRequest(investAccountId);

            assertThatThrownBy(() -> service.createAsset(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不能相同");

            then(transactionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("fromAccountId 不存在，拋出 ResourceNotFoundException，不存任何交易")
        void create_nonExistentFromAccount_throws() {
            UUID unknownId = UUID.randomUUID();
            given(accountRepository.findById(investAccountId)).willReturn(Optional.of(investAccount));
            given(accountRepository.findById(unknownId)).willReturn(Optional.empty());
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any())).willReturn(savedAsset);

            AssetRequest req = buildRequest(unknownId);

            assertThatThrownBy(() -> service.createAsset(req))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(transactionRepository).should(never()).save(any());
        }
    }
}
