package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.MonthlySummaryResponse;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.repository.TransactionRepository;
import com.pocketfolio.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaticsService {

    private final TransactionRepository transactionRepository;

    // 月度收支統計
    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 只查詢當前用戶的交易
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndYearAndMonth(currentUserId, year, month);

        // 分離收入和支出
        Map<Boolean, List<Transaction>> byType = transactions.stream()
                .filter(tx -> tx.getCategory() != null)
                .collect(Collectors.partitioningBy(
                        tx -> tx.getCategory().getType() == CategoryType.INCOME
                ));

        List<Transaction> incomeTransactions = byType.get(true);
        List<Transaction> expenseTransactions = byType.get(false);

        // 計算總收入
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 計算總支出
        BigDecimal totalExpense = expenseTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 計算淨額
        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        // 收入依類別分組統計
        List<MonthlySummaryResponse.CategorySummary> incomeByCategory =
                groupByCategory(incomeTransactions, totalIncome);

        // 支出依類別分組統計
        List<MonthlySummaryResponse.CategorySummary> expenseByCategory =
                groupByCategory(expenseTransactions, totalExpense);

        return MonthlySummaryResponse.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netAmount(netAmount)
                .incomeByCategory(incomeByCategory)
                .expenseByCategory(expenseByCategory)
                .build();
    }

    // Helper: 依類別分組統計
    private List<MonthlySummaryResponse.CategorySummary> groupByCategory(
            List<Transaction> transactions, BigDecimal total
    ) {
        Map<String, BigDecimal> categoryMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        tx -> tx.getCategory().getName(),
                        Collectors.mapping(
                                Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return categoryMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal amount = entry.getValue();
                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : amount.divide(total, 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"))
                                    .setScale(2, RoundingMode.HALF_UP);

                    return MonthlySummaryResponse.CategorySummary.builder()
                            .categoryName(entry.getKey())
                            .amount(amount)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount())) //依金額降序
                .collect(Collectors.toList());
    }
}
