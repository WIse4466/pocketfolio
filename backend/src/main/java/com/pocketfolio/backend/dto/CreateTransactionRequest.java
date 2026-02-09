package com.pocketfolio.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransactionRequest {
    private BigDecimal amount;
    private String note;
    private LocalDate date;
}
