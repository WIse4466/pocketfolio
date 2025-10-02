package com.example.pocketfolio.controller;

import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.dto.TransactionDto;
import com.example.pocketfolio.entity.Transaction;
import com.example.pocketfolio.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionDto> create(@RequestBody CreateTransactionRequest request) {
        Transaction tx = transactionService.createTransaction(request);
        TransactionDto dto = toDto(tx);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<TransactionDto>> list(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<Transaction> p = transactionService.listTransactions(userId, from, to, PageRequest.of(page, size));
        List<TransactionDto> dtos = p.getContent().stream().map(this::toDto).collect(Collectors.toList());
        Page<TransactionDto> dtoPage = new PageImpl<>(dtos, p.getPageable(), p.getTotalElements());
        return ResponseEntity.ok(dtoPage);
    }

    private TransactionDto toDto(Transaction tx) {
        return TransactionDto.builder()
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
    }
}
