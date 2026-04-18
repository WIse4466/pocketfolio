package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.MonthlySummaryResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.repository.TransactionRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaticsService 單元測試")
class StaticsServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private StaticsService service;

    static final UUID CURRENT_USER_ID = UUID.randomUUID();

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

    private Transaction txWith(TransactionType type, String amount, String categoryName) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setDate(LocalDate.of(2026, 1, 15));
        if (categoryName != null) {
            Category c = new Category();
            c.setName(categoryName);
            tx.setCategory(c);
        }
        return tx;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getMonthlySummary
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMonthlySummary（月度統計）")
    class GetMonthlySummary {

        @Test
        @DisplayName("收入與支出正確加總，淨額 = 總收入 - 總支出")
        void summary_correctTotalsAndNetAmount() {
            List<Transaction> transactions = List.of(
                    txWith(TransactionType.INCOME, "5000", "薪資"),
                    txWith(TransactionType.INCOME, "1000", "獎金"),
                    txWith(TransactionType.EXPENSE, "2000", "餐飲"),
                    txWith(TransactionType.EXPENSE, "500", "交通")
            );
            given(transactionRepository.findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 1))
                    .willReturn(transactions);

            MonthlySummaryResponse response = service.getMonthlySummary(2026, 1);

            assertThat(response.getYear()).isEqualTo(2026);
            assertThat(response.getMonth()).isEqualTo(1);
            assertThat(response.getTotalIncome()).isEqualByComparingTo("6000");
            assertThat(response.getTotalExpense()).isEqualByComparingTo("2500");
            assertThat(response.getNetAmount()).isEqualByComparingTo("3500");
        }

        @Test
        @DisplayName("無交易時，所有金額皆為零")
        void summary_emptyTransactions_allZeros() {
            given(transactionRepository.findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 1))
                    .willReturn(List.of());

            MonthlySummaryResponse response = service.getMonthlySummary(2026, 1);

            assertThat(response.getTotalIncome()).isEqualByComparingTo("0");
            assertThat(response.getTotalExpense()).isEqualByComparingTo("0");
            assertThat(response.getNetAmount()).isEqualByComparingTo("0");
            assertThat(response.getIncomeByCategory()).isEmpty();
            assertThat(response.getExpenseByCategory()).isEmpty();
        }

        @Test
        @DisplayName("依類別分組並計算佔比")
        void summary_groupsByCategory_calculatesPercentage() {
            List<Transaction> transactions = List.of(
                    txWith(TransactionType.EXPENSE, "6000", "餐飲"),
                    txWith(TransactionType.EXPENSE, "4000", "交通")
            );
            given(transactionRepository.findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 1))
                    .willReturn(transactions);

            MonthlySummaryResponse response = service.getMonthlySummary(2026, 1);

            assertThat(response.getExpenseByCategory()).hasSize(2);

            MonthlySummaryResponse.CategorySummary dining = response.getExpenseByCategory().stream()
                    .filter(c -> c.getCategoryName().equals("餐飲"))
                    .findFirst()
                    .orElseThrow();
            // 6000 / 10000 = 60%
            assertThat(dining.getPercentage()).isEqualByComparingTo("60.00");
        }

        @Test
        @DisplayName("依類別分組時，無類別的交易被排除在分組之外")
        void summary_nullCategoryTransactions_excludedFromGrouping() {
            List<Transaction> transactions = List.of(
                    txWith(TransactionType.EXPENSE, "3000", "餐飲"),
                    txWith(TransactionType.EXPENSE, "1000", null) // 無類別
            );
            given(transactionRepository.findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 1))
                    .willReturn(transactions);

            MonthlySummaryResponse response = service.getMonthlySummary(2026, 1);

            // 總支出應包含無類別交易
            assertThat(response.getTotalExpense()).isEqualByComparingTo("4000");
            // 但類別分組只有 1 筆
            assertThat(response.getExpenseByCategory()).hasSize(1);
        }

        @Test
        @DisplayName("類別分組依金額降序排列")
        void summary_categoriesSortedByAmountDesc() {
            List<Transaction> transactions = List.of(
                    txWith(TransactionType.EXPENSE, "1000", "交通"),
                    txWith(TransactionType.EXPENSE, "5000", "餐飲"),
                    txWith(TransactionType.EXPENSE, "3000", "娛樂")
            );
            given(transactionRepository.findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 1))
                    .willReturn(transactions);

            MonthlySummaryResponse response = service.getMonthlySummary(2026, 1);

            List<MonthlySummaryResponse.CategorySummary> categories = response.getExpenseByCategory();
            assertThat(categories.get(0).getCategoryName()).isEqualTo("餐飲");
            assertThat(categories.get(2).getCategoryName()).isEqualTo("交通");
        }

        @Test
        @DisplayName("查詢時只帶入當前用戶 ID")
        void summary_queriesCurrentUserOnly() {
            given(transactionRepository.findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 3))
                    .willReturn(List.of());

            service.getMonthlySummary(2026, 3);

            then(transactionRepository).should()
                    .findByUserIdAndYearAndMonth(CURRENT_USER_ID, 2026, 3);
        }
    }
}
