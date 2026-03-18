package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.PriceAlertRequest;
import com.pocketfolio.backend.dto.PriceAlertResponse;
import com.pocketfolio.backend.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Tag(name = "9. 價格警報", description = "設定資產價格達到指定條件時的通知")
@SecurityRequirement(name = "bearerAuth")
public class PriceAlertController {

    private final PriceAlertService service;

    @PostMapping
    @Operation(
            summary = "建立價格警報",
            description = "設定當資產價格高於或低於指定價格時發送通知"
    )
    public ResponseEntity<PriceAlertResponse> createAlert(
            @Valid @RequestBody PriceAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAlert(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單個警報", description = "根據 ID 查詢警報詳情")
    public ResponseEntity<PriceAlertResponse> getAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAlert(id));
    }

    @GetMapping
    @Operation(
            summary = "查詢我的所有警報",
            description = "查詢當前用戶的所有價格警報"
    )
    public ResponseEntity<List<PriceAlertResponse>> getMyAlerts(
            @Parameter(description = "只顯示啟用中的警報")
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        if (activeOnly) {
            return ResponseEntity.ok(service.getUserActiveAlerts());
        }
        return ResponseEntity.ok(service.getUserAlerts());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新警報", description = "修改指定警報的內容（會重置觸發狀態）")
    public ResponseEntity<PriceAlertResponse> updateAlert(
            @PathVariable UUID id,
            @Valid @RequestBody PriceAlertRequest request) {
        return ResponseEntity.ok(service.updateAlert(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(
            summary = "啟用/停用警報",
            description = "切換警報的啟用狀態"
    )
    public ResponseEntity<PriceAlertResponse> toggleAlert(
            @PathVariable UUID id,
            @Parameter(description = "true=啟用, false=停用")
            @RequestParam boolean active) {
        return ResponseEntity.ok(service.toggleAlert(id, active));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除警報", description = "刪除指定的價格警報")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID id) {
        service.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}