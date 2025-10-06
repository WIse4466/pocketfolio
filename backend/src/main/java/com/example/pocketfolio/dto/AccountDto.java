package com.example.pocketfolio.dto;

import com.example.pocketfolio.entity.AccountType;
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
public class AccountDto {
    private UUID id;
    private UUID userId;
    private String name;
    private AccountType type;
    private String currencyCode;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private boolean includeInNetWorth;
    private boolean archived;
    private Integer closingDay;
    private Integer dueDay;
    private UUID autopayAccountId;
    private short dueMonthOffset;
    private String dueHolidayPolicy;
    private boolean autopayEnabled;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

