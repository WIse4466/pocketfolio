package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@Disabled("Phase 3 完成後需重寫測試以支援用戶隔離")
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService 單元測試")
public class TransactionServiceTest {

    @Mock
    TransactionRepository repository;

    @InjectMocks
    TransactionService service;

    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;

    UUID txId;
    Transaction savedTx;

    @BeforeEach
    void setUp() {
        txId = UUID.randomUUID();

        savedTx = new Transaction();
        savedTx.setId(txId);
        savedTx.setAmount(new BigDecimal("1000"));
        savedTx.setNote("薪水");
        savedTx.setDate(LocalDate.of(2026, 2, 15));
    }

    // Create
    @Nested
    @DisplayName("createTransaction")
    class CreateTransactionTests {

        @Test
        @DisplayName("正確將 Request DTO 的所有欄位映射到 Entity 並儲存")
        void create_correctMapping() {
            // Arrange
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("1000"));
            request.setNote("薪水");
            request.setDate(LocalDate.of(2026, 2, 15));

            given(repository.save(any(Transaction.class))).willReturn(savedTx);

            // Act
            TransactionResponse response = service.createTransaction(request);

            // Assert - 使用 Captor 驗證傳給 Repository 的物件
            then(repository).should().save(transactionCaptor.capture());
            Transaction captured = transactionCaptor.getValue();

            assertThat(captured.getAmount()).isEqualByComparingTo(request.getAmount());
            assertThat(captured.getNote()).isEqualTo(request.getNote());
            assertThat(captured.getDate()).isEqualTo(request.getDate());

            // Assert - 驗證回傳的 Response
            assertThat(response.getId()).isEqualTo(savedTx.getId());
            assertThat(response.getAmount()).isEqualByComparingTo("1000");
            assertThat(response.getNote()).isEqualTo("薪水");
            assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        }

        @Test
        @DisplayName("date 為 null 時，自動設定今天日期再儲存")
        void create_withNullDate_setsToday() {
            // Given
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("500"));
            request.setNote("測試");
            request.setDate(null);  // 沒有傳日期

            Transaction txWithToday = new Transaction();
            txWithToday.setId(UUID.randomUUID());
            txWithToday.setAmount(new BigDecimal("500"));
            txWithToday.setNote("測試");
            txWithToday.setDate(LocalDate.now());

            given(repository.save(any(Transaction.class))).willReturn(txWithToday);

            // When
            TransactionResponse response = service.createTransaction(request);

            // Then：驗證傳給 Repository 的 Entity 已經設定了今天日期
            then(repository).should().save(transactionCaptor.capture());
            Transaction captured = transactionCaptor.getValue();

            assertThat(captured.getDate()).isEqualTo(LocalDate.now());
            assertThat(response.getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("note 為 null 也能正常儲存")
        void create_withNullNote_savesSuccessfully() {
            // Given
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("300"));
            request.setNote(null);
            request.setDate(LocalDate.now());

            Transaction txNoNote = new Transaction();
            txNoNote.setId(UUID.randomUUID());
            txNoNote.setAmount(new BigDecimal("300"));
            txNoNote.setNote(null);
            txNoNote.setDate(LocalDate.now());

            given(repository.save(any(Transaction.class))).willReturn(txNoNote);

            // When
            TransactionResponse response = service.createTransaction(request);

            // Then
            then(repository).should().save(transactionCaptor.capture());
            assertThat(transactionCaptor.getValue().getNote()).isNull();
            assertThat(response.getNote()).isNull();
        }

        @Test
        @DisplayName("Repository 的 save 方法只被呼叫一次")
        void create_callsSaveExactlyOnce() {
            // Given
            TransactionRequest request = new TransactionRequest();
            request.setAmount(BigDecimal.TEN);
            request.setDate(LocalDate.now());

            given(repository.save(any(Transaction.class))).willReturn(savedTx);

            // When
            service.createTransaction(request);

            // Then
            then(repository).should(times(1)).save(any(Transaction.class));
        }
    }

    // Read 單筆
    @Nested
    @DisplayName("getTransaction (查詢單筆)")
    class GetTransactionTests {

        @Test
        @DisplayName("查詢存在的 ID，回傳正確 Response")
        void getOne_existingId_returnsCorrectResponse() {
            // Given
            given(repository.findById(txId)).willReturn(Optional.of(savedTx));

            // When
            TransactionResponse response = service.getTransaction(txId);

            // Then
            assertThat(response.getId()).isEqualTo(txId);
            assertThat(response.getAmount()).isEqualByComparingTo("1000");
            assertThat(response.getNote()).isEqualTo("薪水");
            assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 2, 15));

            then(repository).should().findById(txId);
        }

        @Test
        @DisplayName("查詢不存在的 ID，拋出 ResourceNotFoundException")
        void getOne_nonExistingId_throwsResourceNotFoundException() {
            // Given
            UUID unknownId = UUID.randomUUID();
            given(repository.findById(unknownId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getTransaction(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString())
                    .hasMessageContaining("找不到");

            then(repository).should().findById(unknownId);
        }
    }

    // Read 分頁列表
    @Nested
    @DisplayName("getAllTransactions (分頁查詢)")
    class GetAllTransactionsTests {

        @Test
        @DisplayName("分頁查詢，回傳正確的 Page 物件")
        void getAll_returnsPaginatedResult() {
            // Given
            PageRequest pageable = PageRequest.of(0, 10);
            Transaction tx2 = new Transaction();
            tx2.setId(UUID.randomUUID());
            tx2.setAmount(new BigDecimal("500"));
            tx2.setDate(LocalDate.now());

            Page<Transaction> fakePage = new PageImpl<>(
                    List.of(savedTx, tx2),
                    pageable,
                    2  // total elements
            );

            given(repository.findAll(pageable)).willReturn(fakePage);

            // When
            Page<TransactionResponse> result = service.getAllTransactions(pageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo(savedTx.getId());

            then(repository).should().findAll(pageable);
        }

        @Test
        @DisplayName("查詢空結果，回傳空 Page")
        void getAll_emptyResult_returnsEmptyPage() {
            // Given
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Transaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(repository.findAll(pageable)).willReturn(emptyPage);

            // When
            Page<TransactionResponse> result = service.getAllTransactions(pageable);

            // Then
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // Update
    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionTests {

        @Test
        @DisplayName("正常更新，所有欄位都被更新並回傳正確 Response")
        void update_existingId_updatesAllFieldsAndReturnsResponse() {
            // Given
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("999"));
            request.setNote("修正金額");
            request.setDate(LocalDate.of(2026, 3, 1));

            Transaction updatedTx = new Transaction();
            updatedTx.setId(txId);
            updatedTx.setAmount(new BigDecimal("999"));
            updatedTx.setNote("修正金額");
            updatedTx.setDate(LocalDate.of(2026, 3, 1));

            given(repository.findById(txId)).willReturn(Optional.of(savedTx));
            given(repository.save(any(Transaction.class))).willReturn(updatedTx);

            // When
            TransactionResponse response = service.updateTransaction(txId, request);

            // Then：驗證傳給 save 的物件已更新
            then(repository).should().save(transactionCaptor.capture());
            Transaction captured = transactionCaptor.getValue();

            assertThat(captured.getId()).isEqualTo(txId);  // ID 不變
            assertThat(captured.getAmount()).isEqualByComparingTo("999");
            assertThat(captured.getNote()).isEqualTo("修正金額");
            assertThat(captured.getDate()).isEqualTo(LocalDate.of(2026, 3, 1));

            // Then：驗證回傳的 Response
            assertThat(response.getId()).isEqualTo(txId);
            assertThat(response.getAmount()).isEqualByComparingTo("999");
            assertThat(response.getNote()).isEqualTo("修正金額");
        }

        @Test
        @DisplayName("更新時 date 為 null，不覆蓋原本的日期")
        void update_withNullDate_keepsOriginalDate() {
            // Given
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("800"));
            request.setNote("只改金額");
            request.setDate(null);  // 不想改日期

            given(repository.findById(txId)).willReturn(Optional.of(savedTx));
            given(repository.save(any(Transaction.class))).willReturn(savedTx);

            // When
            service.updateTransaction(txId, request);

            // Then：驗證日期沒被改掉
            then(repository).should().save(transactionCaptor.capture());
            Transaction captured = transactionCaptor.getValue();

            assertThat(captured.getDate()).isEqualTo(LocalDate.of(2026, 2, 15));  // 保持原本日期
        }

        @Test
        @DisplayName("更新不存在的 ID，拋出 ResourceNotFoundException")
        void update_nonExistingId_throwsResourceNotFoundException() {
            // Given
            UUID unknownId = UUID.randomUUID();
            TransactionRequest request = new TransactionRequest();
            request.setAmount(new BigDecimal("100"));

            given(repository.findById(unknownId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateTransaction(unknownId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());

            then(repository).should().findById(unknownId);
            then(repository).should(never()).save(any(Transaction.class));
        }
    }

    // Delete
    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransactionTests {

        @Test
        @DisplayName("刪除存在的 ID，Repository 的 deleteById 被呼叫一次")
        void delete_existingId_callsDeleteById() {
            // Given
            given(repository.existsById(txId)).willReturn(true);
            willDoNothing().given(repository).deleteById(txId);

            // When
            service.deleteTransaction(txId);

            // Then
            then(repository).should().existsById(txId);
            then(repository).should().deleteById(txId);
        }

        @Test
        @DisplayName("刪除不存在的 ID，拋出 ResourceNotFoundException 且不呼叫 deleteById")
        void delete_nonExistingId_throwsExceptionWithoutCallingDelete() {
            // Given
            UUID unknownId = UUID.randomUUID();
            given(repository.existsById(unknownId)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.deleteTransaction(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());

            then(repository).should().existsById(unknownId);
            then(repository).should(never()).deleteById(any());  // 確認完全沒呼叫
        }
    }
}
