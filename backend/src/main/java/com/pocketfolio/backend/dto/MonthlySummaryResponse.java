package com.pocketfolio.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MonthlySummaryResponse {
    private int year;
    private int month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netAmount;

    private List<CategorySummary> incomeByCategory;
    private List<CategorySummary> expenseByCategory;

    @Data
    @Builder
    public static class CategorySummary {
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
