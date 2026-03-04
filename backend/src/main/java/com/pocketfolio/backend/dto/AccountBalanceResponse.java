package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AccountBalanceResponse {
    private UUID id;
    private String name;
    private AccountType type;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private BigDecimal change;         //變動金額
    private BigDecimal changePercent;
}
