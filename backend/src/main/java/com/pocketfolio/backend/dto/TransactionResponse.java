package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.AccountType;
import com.pocketfolio.backend.entity.CategoryType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private BigDecimal amount;
    private String note;
    private LocalDate date;

    private UUID categoryId;
    private String categoryName;
    private CategoryType categoryType;

    private UUID accountId;
    private String accountName;
    private AccountType accountType;
}
