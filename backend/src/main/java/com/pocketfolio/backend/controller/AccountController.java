package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AccountRequest;
import com.pocketfolio.backend.dto.AccountResponse;
import com.pocketfolio.backend.entity.AccountType;
import com.pocketfolio.backend.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAccount(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAccount(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAll(
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) String search) {

        if (type != null) {
            return ResponseEntity.ok(service.getAccountsByType(type));
        }

        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(service.searchAccounts(search));
        }

        return ResponseEntity.ok(service.getAllAccounts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(service.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}