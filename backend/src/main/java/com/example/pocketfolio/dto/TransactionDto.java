package com.example.pocketfolio.dto;

import com.example.pocketfolio.entity.TransactionKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private UUID id;
    private UUID userId;
    private TransactionKind kind;
    private BigDecimal amount;
    private Instant occurredAt;
    private UUID accountId;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private UUID categoryId;
    private String notes;
    private String currencyCode;
    private BigDecimal fxRateUsed;
}

