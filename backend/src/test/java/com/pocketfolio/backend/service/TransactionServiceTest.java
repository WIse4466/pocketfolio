package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.CategoryRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
@DisplayName("TransactionService 單元測試")
class TransactionServiceTest {

    @Mock private TransactionRepository repository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AssetRepository assetRepository;

    @InjectMocks private TransactionService service;

    @Captor private ArgumentCaptor<Transaction> txCaptor;

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
        return accountWith(id, ownerId, AccountType.CASH);
    }

    private Account accountWith(UUID id, UUID ownerId, AccountType type) {
        Account a = new Account();
        a.setId(id);
        a.setName("帳戶-" + id.toString().substring(0, 4));
        a.setType(type);
        a.setInitialBalance(BigDecimal.ZERO);
        a.setUser(userWith(ownerId));
        return a;
    }

    private Asset assetWith(UUID id, UUID ownerId, Account account) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setSymbol("2330.TW");
        asset.setName("台積電");
        asset.setType(AssetType.STOCK);
        asset.setQuantity(new BigDecimal("10"));
        asset.setCostPrice(new BigDecimal("800"));
        asset.setCurrentPrice(new BigDecimal("800"));
        asset.setAccount(account);
        asset.setUser(userWith(ownerId));
        return asset;
    }

    private TransactionRequest transferToInvestmentRequest(UUID fromId, UUID toId) {
        TransactionRequest req = transferRequest(fromId, toId);
        return req;
    }

    private Category categoryWith(UUID id, UUID ownerId) {
        Category c = new Category();
        c.setId(id);
        c.setName("類別-" + id.toString().substring(0, 4));
        c.setType(CategoryType.EXPENSE);
        c.setUser(userWith(ownerId));
        return c;
    }

    private Transaction txEntity(UUID id, TransactionType type, UUID ownerId) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(type);
        tx.setAmount(new BigDecimal("1000"));
        tx.setDate(LocalDate.of(2026, 1, 1));
        tx.setUser(userWith(ownerId));
        return tx;
    }

    private TransactionRequest incomeRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.INCOME);
        req.setAmount(new BigDecimal("1000"));
        req.setNote("薪水");
        req.setDate(LocalDate.of(2026, 2, 15));
        return req;
    }

    private TransactionRequest transferRequest(UUID fromAccountId, UUID toAccountId) {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.TRANSFER_OUT);
        req.setAmount(new BigDecimal("500"));
        req.setDate(LocalDate.of(2026, 3, 1));
        req.setAccountId(fromAccountId);
        req.setToAccountId(toAccountId);
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTransaction — INCOME / EXPENSE
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTransaction（一般交易）")
    class CreateNormalTransaction {

        @Test
        @DisplayName("正確將 Request 欄位映射到 Entity 並儲存")
        void create_correctFieldMapping() {
            Transaction result = txEntity(UUID.randomUUID(), TransactionType.INCOME, CURRENT_USER_ID);
            given(repository.save(any())).willReturn(result);

            service.createTransaction(incomeRequest());

            then(repository).should().save(txCaptor.capture());
            Transaction captured = txCaptor.getValue();

            assertThat(captured.getAmount()).isEqualByComparingTo("1000");
            assertThat(captured.getNote()).isEqualTo("薪水");
            assertThat(captured.getDate()).isEqualTo(LocalDate.of(2026, 2, 15));
            assertThat(captured.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(captured.getUser().getId()).isEqualTo(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("date 為 null 時，自動設定為今天")
        void create_withNullDate_setsToday() {
            TransactionRequest req = incomeRequest();
            req.setDate(null);

            Transaction result = txEntity(UUID.randomUUID(), TransactionType.INCOME, CURRENT_USER_ID);
            result.setDate(LocalDate.now());
            given(repository.save(any())).willReturn(result);

            service.createTransaction(req);

            then(repository).should().save(txCaptor.capture());
            assertThat(txCaptor.getValue().getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("指定自己的 category，成功關聯")
        void create_withOwnedCategory_linksCategory() {
            UUID catId = UUID.randomUUID();
            Category category = categoryWith(catId, CURRENT_USER_ID);

            TransactionRequest req = incomeRequest();
            req.setCategoryId(catId);

            Transaction result = txEntity(UUID.randomUUID(), TransactionType.INCOME, CURRENT_USER_ID);
            result.setCategory(category);
            given(categoryRepository.findById(catId)).willReturn(Optional.of(category));
            given(repository.save(any())).willReturn(result);

            service.createTransaction(req);

            then(repository).should().save(txCaptor.capture());
            assertThat(txCaptor.getValue().getCategory().getId()).isEqualTo(catId);
        }

        @Test
        @DisplayName("指定他人的 category，拋出 IllegalArgumentException 且不儲存")
        void create_withOtherUserCategory_throws() {
            UUID catId = UUID.randomUUID();
            given(categoryRepository.findById(catId))
                    .willReturn(Optional.of(categoryWith(catId, OTHER_USER_ID)));

            TransactionRequest req = incomeRequest();
            req.setCategoryId(catId);

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("指定自己的 account，成功關聯")
        void create_withOwnedAccount_linksAccount() {
            UUID accId = UUID.randomUUID();
            Account account = accountWith(accId, CURRENT_USER_ID);

            TransactionRequest req = incomeRequest();
            req.setAccountId(accId);

            Transaction result = txEntity(UUID.randomUUID(), TransactionType.INCOME, CURRENT_USER_ID);
            result.setAccount(account);
            given(accountRepository.findById(accId)).willReturn(Optional.of(account));
            given(repository.save(any())).willReturn(result);

            service.createTransaction(req);

            then(repository).should().save(txCaptor.capture());
            assertThat(txCaptor.getValue().getAccount().getId()).isEqualTo(accId);
        }

        @Test
        @DisplayName("指定他人的 account，拋出 IllegalArgumentException 且不儲存")
        void create_withOtherUserAccount_throws() {
            UUID accId = UUID.randomUUID();
            given(accountRepository.findById(accId))
                    .willReturn(Optional.of(accountWith(accId, OTHER_USER_ID)));

            TransactionRequest req = incomeRequest();
            req.setAccountId(accId);

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");

            then(repository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTransaction — TRANSFER
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTransaction（轉帳）")
    class CreateTransferTransaction {

        @Test
        @DisplayName("成功建立 TRANSFER_OUT + TRANSFER_IN 一對，共用同一個 transferGroupId")
        void createTransfer_happyPath_createsBothRecordsWithSameGroupId() {
            UUID fromId = UUID.randomUUID();
            UUID toId   = UUID.randomUUID();
            Account fromAccount = accountWith(fromId, CURRENT_USER_ID);
            Account toAccount   = accountWith(toId,   CURRENT_USER_ID);

            given(accountRepository.findById(fromId)).willReturn(Optional.of(fromAccount));
            given(accountRepository.findById(toId)).willReturn(Optional.of(toAccount));

            UUID groupId   = UUID.randomUUID();
            Transaction inResult  = txEntity(UUID.randomUUID(), TransactionType.TRANSFER_IN,  CURRENT_USER_ID);
            Transaction outResult = txEntity(UUID.randomUUID(), TransactionType.TRANSFER_OUT, CURRENT_USER_ID);
            inResult.setTransferGroupId(groupId);
            outResult.setTransferGroupId(groupId);
            outResult.setAccount(fromAccount);
            given(repository.save(any())).willReturn(inResult, outResult);

            TransactionResponse response = service.createTransaction(transferRequest(fromId, toId));

            // save 被呼叫 2 次
            then(repository).should(times(2)).save(txCaptor.capture());
            List<Transaction> saved = txCaptor.getAllValues();

            assertThat(saved).extracting(Transaction::getType)
                    .containsExactlyInAnyOrder(TransactionType.TRANSFER_IN, TransactionType.TRANSFER_OUT);

            // 兩筆使用同一個 groupId
            assertThat(saved.get(0).getTransferGroupId())
                    .isEqualTo(saved.get(1).getTransferGroupId());

            // Response 帶目標帳戶資訊
            assertThat(response.getToAccountId()).isEqualTo(toId);
        }

        @Test
        @DisplayName("未提供來源帳戶（accountId = null），拋出 IllegalArgumentException")
        void createTransfer_missingFromAccount_throws() {
            TransactionRequest req = new TransactionRequest();
            req.setType(TransactionType.TRANSFER_OUT);
            req.setAmount(new BigDecimal("100"));
            req.setToAccountId(UUID.randomUUID());

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("來源帳戶");
        }

        @Test
        @DisplayName("未提供目標帳戶（toAccountId = null），拋出 IllegalArgumentException")
        void createTransfer_missingToAccount_throws() {
            TransactionRequest req = new TransactionRequest();
            req.setType(TransactionType.TRANSFER_OUT);
            req.setAmount(new BigDecimal("100"));
            req.setAccountId(UUID.randomUUID());

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("目標帳戶");
        }

        @Test
        @DisplayName("來源帳戶與目標帳戶相同，拋出 IllegalArgumentException")
        void createTransfer_sameAccount_throws() {
            UUID sameId = UUID.randomUUID();
            assertThatThrownBy(() -> service.createTransaction(transferRequest(sameId, sameId)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("相同");
        }

        @Test
        @DisplayName("來源帳戶屬於他人，拋出 IllegalArgumentException 且不儲存")
        void createTransfer_fromAccountOtherUser_throws() {
            UUID fromId = UUID.randomUUID();
            UUID toId   = UUID.randomUUID();
            given(accountRepository.findById(fromId))
                    .willReturn(Optional.of(accountWith(fromId, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.createTransaction(transferRequest(fromId, toId)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("目標帳戶屬於他人，拋出 IllegalArgumentException 且不儲存")
        void createTransfer_toAccountOtherUser_throws() {
            UUID fromId = UUID.randomUUID();
            UUID toId   = UUID.randomUUID();
            given(accountRepository.findById(fromId))
                    .willReturn(Optional.of(accountWith(fromId, CURRENT_USER_ID)));
            given(accountRepository.findById(toId))
                    .willReturn(Optional.of(accountWith(toId, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.createTransaction(transferRequest(fromId, toId)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");

            then(repository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTransaction（查詢單筆）")
    class GetTransaction {

        @Test
        @DisplayName("查詢自己的交易，回傳正確 Response")
        void getOne_ownedByCurrentUser_returnsResponse() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.INCOME, CURRENT_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            TransactionResponse response = service.getTransaction(txId);

            assertThat(response.getId()).isEqualTo(txId);
        }

        @Test
        @DisplayName("查詢他人的交易，拋出 ResourceNotFoundException（安全遮蔽，不洩漏存在性）")
        void getOne_ownedByOtherUser_throwsResourceNotFoundException() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.INCOME, OTHER_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.getTransaction(txId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("查詢不存在的 ID，拋出 ResourceNotFoundException")
        void getOne_notFound_throws() {
            UUID unknownId = UUID.randomUUID();
            given(repository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransaction(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllTransactions
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllTransactions（分頁列表）")
    class GetAllTransactions {

        @Test
        @DisplayName("查詢時只帶入當前用戶 ID，不查詢全表")
        void getAll_queriesCurrentUserOnly() {
            PageRequest pageable = PageRequest.of(0, 10);
            given(repository.findByUserId(CURRENT_USER_ID, pageable))
                    .willReturn(new PageImpl<>(List.of()));

            service.getAllTransactions(pageable);

            then(repository).should().findByUserId(CURRENT_USER_ID, pageable);
            then(repository).should(never()).findAll(pageable);
        }

        @Test
        @DisplayName("無資料時回傳空 Page")
        void getAll_emptyResult_returnsEmptyPage() {
            PageRequest pageable = PageRequest.of(0, 10);
            given(repository.findByUserId(CURRENT_USER_ID, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<TransactionResponse> result = service.getAllTransactions(pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTransaction（更新）")
    class UpdateTransaction {

        @Test
        @DisplayName("正常更新欄位並儲存")
        void update_happyPath_updatesFields() {
            UUID txId = UUID.randomUUID();
            Transaction existing = txEntity(txId, TransactionType.INCOME, CURRENT_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(existing));
            given(repository.save(any())).willReturn(existing);

            TransactionRequest req = new TransactionRequest();
            req.setType(TransactionType.EXPENSE);
            req.setAmount(new BigDecimal("999"));
            req.setNote("修改");
            req.setDate(LocalDate.of(2026, 4, 1));

            service.updateTransaction(txId, req);

            then(repository).should().save(txCaptor.capture());
            Transaction captured = txCaptor.getValue();
            assertThat(captured.getAmount()).isEqualByComparingTo("999");
            assertThat(captured.getNote()).isEqualTo("修改");
            assertThat(captured.getDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("date 為 null 時，保留原本日期不覆蓋")
        void update_withNullDate_keepsOriginalDate() {
            UUID txId = UUID.randomUUID();
            Transaction existing = txEntity(txId, TransactionType.INCOME, CURRENT_USER_ID);
            LocalDate originalDate = existing.getDate();
            given(repository.findById(txId)).willReturn(Optional.of(existing));
            given(repository.save(any())).willReturn(existing);

            TransactionRequest req = new TransactionRequest();
            req.setType(TransactionType.INCOME);
            req.setAmount(new BigDecimal("100"));
            req.setDate(null);

            service.updateTransaction(txId, req);

            then(repository).should().save(txCaptor.capture());
            assertThat(txCaptor.getValue().getDate()).isEqualTo(originalDate);
        }

        @Test
        @DisplayName("嘗試編輯 TRANSFER_OUT，拋出 IllegalArgumentException 且不儲存")
        void update_transferOut_throws() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.TRANSFER_OUT, CURRENT_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.updateTransaction(txId, incomeRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("轉帳");

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("嘗試編輯 TRANSFER_IN，拋出 IllegalArgumentException 且不儲存")
        void update_transferIn_throws() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.TRANSFER_IN, CURRENT_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.updateTransaction(txId, incomeRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("轉帳");

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("更新他人的交易，拋出 ResourceNotFoundException 且不儲存")
        void update_ownedByOtherUser_throws() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.INCOME, OTHER_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.updateTransaction(txId, incomeRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("更新不存在的交易，拋出 ResourceNotFoundException")
        void update_notFound_throws() {
            UUID unknownId = UUID.randomUUID();
            given(repository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTransaction(unknownId, incomeRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTransaction（刪除）")
    class DeleteTransaction {

        @Test
        @DisplayName("刪除一般交易，只呼叫 deleteById，不查詢配對記錄")
        void delete_normalTx_deletesById() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.EXPENSE, CURRENT_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            service.deleteTransaction(txId);

            then(repository).should().deleteById(txId);
            then(repository).should(never()).findByTransferGroupIdAndIdNot(any(), any());
        }

        @Test
        @DisplayName("刪除 TRANSFER_OUT，同時串聯刪除配對的 TRANSFER_IN")
        void delete_transferOut_alsoDeletesPairedRecord() {
            UUID txId     = UUID.randomUUID();
            UUID groupId  = UUID.randomUUID();
            UUID pairedId = UUID.randomUUID();

            Transaction tx = txEntity(txId, TransactionType.TRANSFER_OUT, CURRENT_USER_ID);
            tx.setTransferGroupId(groupId);

            Transaction paired = txEntity(pairedId, TransactionType.TRANSFER_IN, CURRENT_USER_ID);
            paired.setTransferGroupId(groupId);

            given(repository.findById(txId)).willReturn(Optional.of(tx));
            given(repository.findByTransferGroupIdAndIdNot(groupId, txId))
                    .willReturn(Optional.of(paired));

            service.deleteTransaction(txId);

            then(repository).should().delete(paired);
            then(repository).should().deleteById(txId);
        }

        @Test
        @DisplayName("刪除他人的交易，拋出 ResourceNotFoundException 且不執行刪除")
        void delete_ownedByOtherUser_throws() {
            UUID txId = UUID.randomUUID();
            Transaction tx = txEntity(txId, TransactionType.INCOME, OTHER_USER_ID);
            given(repository.findById(txId)).willReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.deleteTransaction(txId))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(repository).should(never()).deleteById(any());
        }

        @Test
        @DisplayName("刪除不存在的交易，拋出 ResourceNotFoundException")
        void delete_notFound_throws() {
            UUID unknownId = UUID.randomUUID();
            given(repository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteTransaction(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTransfer — 資產連結
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTransfer（資產連結）")
    class CreateTransferWithAssetLink {

        private UUID fromId;
        private UUID toId;
        private Account fromAccount;
        private Account toAccount;

        @BeforeEach
        void setUp() {
            fromId = UUID.randomUUID();
            toId   = UUID.randomUUID();
            fromAccount = accountWith(fromId, CURRENT_USER_ID, AccountType.CASH);
            toAccount   = accountWith(toId,   CURRENT_USER_ID, AccountType.INVESTMENT);

            given(accountRepository.findById(fromId)).willReturn(Optional.of(fromAccount));
            given(accountRepository.findById(toId)).willReturn(Optional.of(toAccount));

            UUID groupId   = UUID.randomUUID();
            Transaction inResult  = txEntity(UUID.randomUUID(), TransactionType.TRANSFER_IN,  CURRENT_USER_ID);
            Transaction outResult = txEntity(UUID.randomUUID(), TransactionType.TRANSFER_OUT, CURRENT_USER_ID);
            inResult.setTransferGroupId(groupId);
            outResult.setTransferGroupId(groupId);
            outResult.setAccount(fromAccount);
            given(repository.save(any())).willReturn(inResult, outResult);
        }

        @Test
        @DisplayName("目標帳戶非 INVESTMENT，不呼叫 assetRepository")
        void linkAsset_nonInvestmentAccount_noAssetOperation() {
            Account cashTo = accountWith(toId, CURRENT_USER_ID, AccountType.CASH);
            given(accountRepository.findById(toId)).willReturn(Optional.of(cashTo));

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetSymbol("2330.TW");
            req.setAssetQuantity(new BigDecimal("10"));

            service.createTransaction(req);

            then(assetRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("帶有 assetId → 加倉已有資產，更新數量與加權平均成本")
        void linkAsset_withAssetId_updatesQuantityAndWeightedCost() {
            UUID assetId = UUID.randomUUID();
            Asset existing = assetWith(assetId, CURRENT_USER_ID, toAccount);
            // 原本 10 股，成本 800
            given(assetRepository.findById(assetId)).willReturn(Optional.of(existing));
            given(assetRepository.save(any(Asset.class))).willReturn(existing);

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetId(assetId);
            req.setAssetQuantity(new BigDecimal("10"));
            req.setAssetCostPrice(new BigDecimal("900")); // 單價 900，10 股共 9000

            service.createTransaction(req);

            ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
            then(assetRepository).should().save(assetCaptor.capture());
            Asset saved = assetCaptor.getValue();

            assertThat(saved.getQuantity()).isEqualByComparingTo("20");
            // 加權平均：(10×800 + 10×900) / 20 = 850
            assertThat(saved.getCostPrice()).isEqualByComparingTo("850");
        }

        @Test
        @DisplayName("帶有 assetSymbol → 建立新資產")
        void linkAsset_withNewSymbol_createsAsset() {
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(any(), any(), any())).willReturn(false);
            given(assetRepository.save(any(Asset.class))).willAnswer(inv -> inv.getArgument(0));

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetSymbol("nvda");
            req.setAssetName("NVIDIA");
            req.setAssetType(AssetType.STOCK);
            req.setAssetQuantity(new BigDecimal("5"));
            req.setAssetCostPrice(new BigDecimal("1000")); // 單價 1000

            service.createTransaction(req);

            ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
            then(assetRepository).should().save(assetCaptor.capture());
            Asset saved = assetCaptor.getValue();

            assertThat(saved.getSymbol()).isEqualTo("NVDA");
            assertThat(saved.getQuantity()).isEqualByComparingTo("5");
            assertThat(saved.getCostPrice()).isEqualByComparingTo("1000");
        }

        @Test
        @DisplayName("assetQuantity 為 0，拋出 IllegalArgumentException")
        void linkAsset_zeroQuantity_throws() {
            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetSymbol("NVDA");
            req.setAssetQuantity(BigDecimal.ZERO);

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("數量");
        }

        @Test
        @DisplayName("assetId 不存在，拋出 ResourceNotFoundException")
        void linkAsset_assetIdNotFound_throws() {
            UUID unknownAssetId = UUID.randomUUID();
            given(assetRepository.findById(unknownAssetId)).willReturn(Optional.empty());

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetId(unknownAssetId);
            req.setAssetQuantity(new BigDecimal("5"));

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("assetId 屬於他人，拋出 IllegalArgumentException")
        void linkAsset_assetOwnedByOther_throws() {
            UUID assetId = UUID.randomUUID();
            Asset otherAsset = assetWith(assetId, OTHER_USER_ID, toAccount);
            given(assetRepository.findById(assetId)).willReturn(Optional.of(otherAsset));

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetId(assetId);
            req.setAssetQuantity(new BigDecimal("5"));

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("無權");
        }

        @Test
        @DisplayName("assetId 屬於其他帳戶，拋出 IllegalArgumentException")
        void linkAsset_assetInDifferentAccount_throws() {
            UUID assetId = UUID.randomUUID();
            Account otherAccount = accountWith(UUID.randomUUID(), CURRENT_USER_ID, AccountType.INVESTMENT);
            Asset wrongAccountAsset = assetWith(assetId, CURRENT_USER_ID, otherAccount);
            given(assetRepository.findById(assetId)).willReturn(Optional.of(wrongAccountAsset));

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetId(assetId);
            req.setAssetQuantity(new BigDecimal("5"));

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不屬於");
        }

        @Test
        @DisplayName("新資產代號重複，拋出 IllegalArgumentException")
        void linkAsset_duplicateSymbol_throws() {
            given(assetRepository.existsByUserIdAndAccountIdAndSymbol(CURRENT_USER_ID, toId, "NVDA"))
                    .willReturn(true);

            TransactionRequest req = transferRequest(fromId, toId);
            req.setAssetSymbol("NVDA");
            req.setAssetQuantity(new BigDecimal("5"));
            req.setAssetCostPrice(new BigDecimal("1000"));

            assertThatThrownBy(() -> service.createTransaction(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("加倉");
        }
    }
}