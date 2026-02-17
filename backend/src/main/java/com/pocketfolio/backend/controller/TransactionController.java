package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    // POST /api/transactions
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTransaction(request));
    }

    // GET /api/transactions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getTransaction(id));
    }

    // GET /api/transactions?page=0&size=10&sort=date, desc
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAll(
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(service.getAllTransactions(pageable));
    }

    // PUT /api/transactions/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.updateTransaction(id, request));
    }

    // DELETE /api/transactions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteTransaction(id);
        return ResponseEntity.noContent().build();    // 204 No Content
    }

}
