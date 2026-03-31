package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionRequest {
    @NotNull(message = "金額不能為空")
    private BigDecimal amount;

    private String note;

    private LocalDate date;

    @NotNull(message = "交易類型不能為空")
    private TransactionType type;

    // INCOME / EXPENSE 使用；TRANSFER 不需要
    private UUID categoryId;

    // 來源帳戶（所有類型）
    private UUID accountId;

    // 目標帳戶（TRANSFER_OUT 類型使用）
    private UUID toAccountId;
}
