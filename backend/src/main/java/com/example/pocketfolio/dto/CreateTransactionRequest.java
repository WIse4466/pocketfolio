package com.example.pocketfolio.dto;

import com.example.pocketfolio.entity.TransactionKind;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CreateTransactionRequest {
    private UUID userId;
    private TransactionKind kind;
    private BigDecimal amount;
    private Instant occurredAt;

    private UUID accountId; // for income/expense
    private UUID sourceAccountId; // for transfer
    private UUID targetAccountId; // for transfer

    private UUID categoryId; // optional for income/expense
    private String notes;

    private String currencyCode; // required: MVP same-currency only
    private BigDecimal fxRateUsed; // optional, reserved
}

