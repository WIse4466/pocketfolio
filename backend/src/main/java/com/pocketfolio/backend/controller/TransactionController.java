package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.CreateTransactionRequest;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @PostMapping
    public Transaction create(@RequestBody CreateTransactionRequest request) {
        return service.createTransaction(request);
    }
}
