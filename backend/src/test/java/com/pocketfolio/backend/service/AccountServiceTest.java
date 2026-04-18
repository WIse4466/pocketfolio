package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AccountRequest;
import com.pocketfolio.backend.dto.AccountResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
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
@DisplayName("AccountService 單元測試")
class AccountServiceTest {

    @Mock private AccountRepository repository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private AccountService service;

    @Captor private ArgumentCaptor<Account> accountCaptor;

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

    private Account accountWith(UUID id, UUID ownerId) {
        Account a = new Account();
        a.setId(id);
        a.setName("帳戶-" + id.toString().substring(0, 4));
        a.setType(AccountType.CASH);
        a.setInitialBalance(BigDecimal.ZERO);
        a.setUser(userWith(ownerId));
        return a;
    }

    private AccountRequest accountRequest() {
        AccountRequest req = new AccountRequest();
        req.setName("現金");
        req.setType(AccountType.CASH);
        req.setInitialBalance(BigDecimal.valueOf(1000));
        req.setDescription("零用錢");
        req.setCurrency("TWD");
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createAccount
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAccount（建立帳戶）")
    class CreateAccount {

        @Test
        @DisplayName("正確將 Request 欄位映射到 Entity 並儲存")
        void create_happyPath_savesWithCorrectFields() {
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "現金")).willReturn(false);
            Account saved = accountWith(UUID.randomUUID(), CURRENT_USER_ID);
            saved.setName("現金");
            saved.setInitialBalance(BigDecimal.valueOf(1000));
            given(repository.save(any())).willReturn(saved);
            given(transactionRepository.calculateNetAmountByAccountIdAndUserId(any(), any()))
                    .willReturn(BigDecimal.ZERO);

            service.createAccount(accountRequest());

            then(repository).should().save(accountCaptor.capture());
            Account captured = accountCaptor.getValue();
            assertThat(captured.getName()).isEqualTo("現金");
            assertThat(captured.getType()).isEqualTo(AccountType.CASH);
            assertThat(captured.getInitialBalance()).isEqualByComparingTo("1000");
            assertThat(captured.getUser().getId()).isEqualTo(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("帳戶名稱重複：拋出 IllegalArgumentException 且不儲存")
        void create_duplicateName_throws() {
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "現金")).willReturn(true);

            assertThatThrownBy(() -> service.createAccount(accountRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("現金");

            then(repository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAccount
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAccount（查詢單筆）")
    class GetAccount {

        @Test
        @DisplayName("查詢自己的帳戶，回傳正確 Response")
        void getOne_ownedByCurrentUser_returnsResponse() {
            UUID id = UUID.randomUUID();
            Account account = accountWith(id, CURRENT_USER_ID);
            given(repository.findById(id)).willReturn(Optional.of(account));
            given(transactionRepository.calculateNetAmountByAccountIdAndUserId(id, CURRENT_USER_ID))
                    .willReturn(BigDecimal.ZERO);

            AccountResponse response = service.getAccount(id);

            assertThat(response.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("查詢他人的帳戶：拋出 ResourceNotFoundException（安全遮蔽）")
        void getOne_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(accountWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.getAccount(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("查詢不存在的帳戶：拋出 ResourceNotFoundException")
        void getOne_notFound_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAccount(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllAccounts
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllAccounts（查詢所有）")
    class GetAllAccounts {

        @Test
        @DisplayName("查詢時只帶入當前用戶 ID")
        void getAll_queriesCurrentUserOnly() {
            given(repository.findByUserId(CURRENT_USER_ID)).willReturn(List.of());

            service.getAllAccounts();

            then(repository).should().findByUserId(CURRENT_USER_ID);
            then(repository).should(never()).findAll();
        }

        @Test
        @DisplayName("無資料時回傳空 List")
        void getAll_emptyResult_returnsEmptyList() {
            given(repository.findByUserId(CURRENT_USER_ID)).willReturn(List.of());

            assertThat(service.getAllAccounts()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAccount
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAccount（更新）")
    class UpdateAccount {

        @Test
        @DisplayName("正常更新欄位並儲存")
        void update_happyPath_updatesFields() {
            UUID id = UUID.randomUUID();
            Account existing = accountWith(id, CURRENT_USER_ID);
            existing.setName("舊名稱");
            given(repository.findById(id)).willReturn(Optional.of(existing));
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "現金")).willReturn(false);
            Account saved = accountWith(id, CURRENT_USER_ID);
            saved.setName("現金");
            saved.setInitialBalance(BigDecimal.valueOf(1000));
            given(repository.save(any())).willReturn(saved);
            given(transactionRepository.calculateNetAmountByAccountIdAndUserId(any(), any()))
                    .willReturn(BigDecimal.ZERO);

            service.updateAccount(id, accountRequest());

            then(repository).should().save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getName()).isEqualTo("現金");
            assertThat(accountCaptor.getValue().getInitialBalance()).isEqualByComparingTo("1000");
        }

        @Test
        @DisplayName("改名為已存在名稱：拋出 IllegalArgumentException 且不儲存")
        void update_duplicateName_throws() {
            UUID id = UUID.randomUUID();
            Account existing = accountWith(id, CURRENT_USER_ID);
            existing.setName("舊名稱");
            given(repository.findById(id)).willReturn(Optional.of(existing));
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "現金")).willReturn(true);

            assertThatThrownBy(() -> service.updateAccount(id, accountRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("現金");

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("名稱未變更：不做重複檢查，直接儲存")
        void update_sameName_noConflictCheck() {
            UUID id = UUID.randomUUID();
            Account existing = accountWith(id, CURRENT_USER_ID);
            existing.setName("現金");
            given(repository.findById(id)).willReturn(Optional.of(existing));
            Account saved = accountWith(id, CURRENT_USER_ID);
            saved.setName("現金");
            saved.setInitialBalance(BigDecimal.valueOf(1000));
            given(repository.save(any())).willReturn(saved);
            given(transactionRepository.calculateNetAmountByAccountIdAndUserId(any(), any()))
                    .willReturn(BigDecimal.ZERO);

            service.updateAccount(id, accountRequest());

            then(repository).should(never()).existsByUserIdAndName(any(), any());
            then(repository).should().save(any());
        }

        @Test
        @DisplayName("更新他人的帳戶：拋出 ResourceNotFoundException 且不儲存")
        void update_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(accountWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.updateAccount(id, accountRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(repository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAccount
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAccount（刪除）")
    class DeleteAccount {

        @Test
        @DisplayName("成功刪除自己的帳戶")
        void delete_happyPath_deletesById() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(accountWith(id, CURRENT_USER_ID)));

            service.deleteAccount(id);

            then(repository).should().deleteById(id);
        }

        @Test
        @DisplayName("刪除他人帳戶：拋出 ResourceNotFoundException 且不執行刪除")
        void delete_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(accountWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.deleteAccount(id))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(repository).should(never()).deleteById(any());
        }

        @Test
        @DisplayName("刪除不存在帳戶：拋出 ResourceNotFoundException")
        void delete_notFound_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAccount(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateCurrentBalance
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateCurrentBalance（餘額計算）")
    class CalculateBalance {

        @Test
        @DisplayName("一般帳戶：initialBalance + 淨交易金額")
        void balance_nonInvestment_isInitialPlusNet() {
            UUID id = UUID.randomUUID();
            Account account = accountWith(id, CURRENT_USER_ID);
            account.setType(AccountType.BANK);
            account.setInitialBalance(new BigDecimal("5000"));
            given(repository.findById(id)).willReturn(Optional.of(account));
            given(transactionRepository.calculateNetAmountByAccountIdAndUserId(id, CURRENT_USER_ID))
                    .willReturn(new BigDecimal("1500"));

            AccountResponse response = service.getAccount(id);

            assertThat(response.getCurrentBalance()).isEqualByComparingTo("6500");
        }

        @Test
        @DisplayName("投資帳戶：餘額為所有資產市值加總")
        void balance_investment_isSumOfAssetMarketValues() {
            UUID id = UUID.randomUUID();
            Account account = accountWith(id, CURRENT_USER_ID);
            account.setType(AccountType.INVESTMENT);
            account.setInitialBalance(BigDecimal.ZERO);

            Asset a1 = new Asset();
            a1.setUser(userWith(CURRENT_USER_ID));
            a1.setQuantity(new BigDecimal("10"));
            a1.setCostPrice(new BigDecimal("100"));
            a1.setCurrentPrice(new BigDecimal("200"));

            Asset a2 = new Asset();
            a2.setUser(userWith(CURRENT_USER_ID));
            a2.setQuantity(new BigDecimal("5"));
            a2.setCostPrice(new BigDecimal("50"));
            a2.setCurrentPrice(new BigDecimal("80"));

            account.getAssets().add(a1);
            account.getAssets().add(a2);

            given(repository.findById(id)).willReturn(Optional.of(account));

            AccountResponse response = service.getAccount(id);

            // a1 market value = 10 * 200 = 2000, a2 = 5 * 80 = 400 → total = 2400
            assertThat(response.getCurrentBalance()).isEqualByComparingTo("2400");
        }
    }
}
