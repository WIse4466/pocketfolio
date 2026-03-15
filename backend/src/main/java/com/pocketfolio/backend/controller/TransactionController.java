package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.TransactionRequest;
import com.pocketfolio.backend.dto.TransactionResponse;
import com.pocketfolio.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "2. 交易記錄", description = "管理收支交易記錄")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService service;

    // POST /api/transactions
    @PostMapping
    @Operation(summary = "建立交易", description = "新增一筆收支交易記錄")
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTransaction(request));
    }

    // GET /api/transactions/{id}
    @GetMapping("/{id}")
    @Operation(summary = "查詢單筆交易", description = "根據 ID 查詢交易詳情")
    public ResponseEntity<TransactionResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getTransaction(id));
    }

    // GET /api/transactions?page=0&size=10&sort=date, desc
    @GetMapping
    @Operation(
            summary = "查詢交易列表",
            description = "支援分頁、排序及多種篩選條件（類別、帳戶、日期範圍）"
    )
    public ResponseEntity<Page<TransactionResponse>> getAll(
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (categoryId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(service.getTransactionsByCategoryAndDateRange(
                    categoryId, startDate, endDate, pageable
            ));
        }

        if (accountId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(service.getTransactionsByAccountAndDateRange(
                    accountId, startDate, endDate, pageable
            ));
        }

        if (categoryId != null) {
            return ResponseEntity.ok(service.getTransactionsByCategory(categoryId, pageable));
        }

        if (accountId != null) {
            return ResponseEntity.ok(service.getTransactionsByAccount(accountId, pageable));
        }

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(service.getTransactionsByDateRange(startDate, endDate, pageable));
        }

        return ResponseEntity.ok(service.getAllTransactions(pageable));
    }

    // PUT /api/transactions/{id}
    @PutMapping("/{id}")
    @Operation(summary = "更新交易", description = "修改指定交易的內容")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.updateTransaction(id, request));
    }

    // DELETE /api/transactions/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除交易", description = "刪除指定的交易記錄")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteTransaction(id);
        return ResponseEntity.noContent().build();    // 204 No Content
    }

}
