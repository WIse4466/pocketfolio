package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountRequest {

    @NotBlank(message = "帳戶名稱不能為空")
    private String name;

    @NotNull(message = "帳戶類型不能為空")
    private AccountType type;

    @NotNull(message = "初始餘額不能為空")
    private BigDecimal initialBalance;

    private String description;

    private String currency = "TWD";  // 預設新台幣
}