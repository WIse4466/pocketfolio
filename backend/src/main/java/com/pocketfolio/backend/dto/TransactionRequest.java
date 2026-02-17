package com.pocketfolio.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {
    @NotNull(message = "金額不能為空")
    private BigDecimal amount;
    private String note;
    private LocalDate date;
}
