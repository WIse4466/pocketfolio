package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AccountType;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.entity.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private TransactionType type;
    private BigDecimal amount;
    private String note;
    private LocalDate date;
    private UUID transferGroupId;

    private UUID categoryId;
    private String categoryName;
    private CategoryType categoryType;

    private UUID accountId;
    private String accountName;
    private AccountType accountType;

    // 轉帳目標帳戶（type = TRANSFER_OUT 時有值）
    private UUID toAccountId;
    private String toAccountName;
}
