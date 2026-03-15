package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AccountRequest;
import com.pocketfolio.backend.dto.AccountResponse;
import com.pocketfolio.backend.entity.AccountType;
import com.pocketfolio.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "4. 帳戶管理", description = "管理銀行、現金、信用卡、投資帳戶")
@SecurityRequirement(name = "bearerAuth")

public class AccountController {

    private final AccountService service;

    @PostMapping
    @Operation(summary = "建立帳戶", description = "新增一個財務帳戶（銀行、現金、信用卡、投資）")
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAccount(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單個帳戶", description = "根據 ID 查詢帳戶詳情，包含當前餘額")
    public ResponseEntity<AccountResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAccount(id));
    }

    @GetMapping
    @Operation(
            summary = "查詢帳戶列表",
            description = "查詢所有帳戶，可依類型篩選或搜尋名稱"
    )
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
    @Operation(summary = "更新帳戶", description = "修改指定帳戶的內容")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(service.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除帳戶", description = "刪除指定的帳戶")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}