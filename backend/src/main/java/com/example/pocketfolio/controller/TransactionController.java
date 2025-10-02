package com.example.pocketfolio.controller;

import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.dto.TransactionDto;
import com.example.pocketfolio.entity.Transaction;
import com.example.pocketfolio.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionDto> create(@RequestBody CreateTransactionRequest request) {
        Transaction tx = transactionService.createTransaction(request);
        TransactionDto dto = TransactionDto.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .kind(tx.getKind())
                .amount(tx.getAmount())
                .occurredAt(tx.getOccurredAt())
                .accountId(tx.getAccount() != null ? tx.getAccount().getId() : null)
                .sourceAccountId(tx.getSourceAccount() != null ? tx.getSourceAccount().getId() : null)
                .targetAccountId(tx.getTargetAccount() != null ? tx.getTargetAccount().getId() : null)
                .categoryId(tx.getCategory() != null ? tx.getCategory().getId() : null)
                .notes(tx.getNotes())
                .currencyCode(tx.getCurrencyCode())
                .fxRateUsed(tx.getFxRateUsed())
                .build();
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }
}

