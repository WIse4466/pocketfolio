package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.PriceAlertRequest;
import com.pocketfolio.backend.dto.PriceAlertResponse;
import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.service.PriceAlertService;
import com.pocketfolio.backend.service.PriceService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/price-alerts")
@RequiredArgsConstructor
@Tag(name = "9. 價格警報", description = "設定資產價格達到指定條件時的通知")
@SecurityRequirement(name = "bearerAuth")
public class PriceAlertController {

    private final PriceAlertService alertService;
    private final PriceService priceService;

    @PostMapping
    @Operation(
            summary = "建立價格警報",
            description = "設定當資產價格高於或低於指定價格時發送通知"
    )
    public ResponseEntity<PriceAlertResponse> createAlert(
            @Valid @RequestBody PriceAlertRequest request) {

        PriceAlertResponse response = alertService.createAlert(request);

        response = enrichWithCurrentPrice(response);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單個警報", description = "根據 ID 查詢警報詳情")
    public ResponseEntity<PriceAlertResponse> getAlert(@PathVariable UUID id) {

        PriceAlertResponse response = alertService.getAlert(id);

        response = enrichWithCurrentPrice(response);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "查詢我的所有警報",
            description = "查詢當前用戶的所有價格警報"
    )
    public ResponseEntity<List<PriceAlertResponse>> getMyAlerts(
            @Parameter(description = "只顯示啟用中的警報")
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<PriceAlertResponse> alerts = activeOnly
                ? alertService.getUserActiveAlerts()
                : alertService.getUserAlerts();

        // ✅ 批次補上當前價格
        List<PriceAlertResponse> enrichedAlerts = alerts.stream()
                .map(this::enrichWithCurrentPrice)
                .collect(Collectors.toList());

        return ResponseEntity.ok(enrichedAlerts);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新警報", description = "修改指定警報的內容（會重置觸發狀態）")
    public ResponseEntity<PriceAlertResponse> updateAlert(
            @PathVariable UUID id,
            @Valid @RequestBody PriceAlertRequest request) {

        PriceAlertResponse response = alertService.updateAlert(id, request);
        response = enrichWithCurrentPrice(response);

        return ResponseEntity.ok(response);
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

        PriceAlertResponse response = alertService.toggleAlert(id, active);
        response = enrichWithCurrentPrice(response);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除警報", description = "刪除指定的價格警報")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helper: 補充當前價格 ──────────────────────────────
    private PriceAlertResponse enrichWithCurrentPrice(PriceAlertResponse response) {
        try {
            PriceData priceData = priceService.getPrice(
                    response.getSymbol(),
                    response.getAssetType()
            );

            if (priceData != null && priceData.getPrice() != null) {
                response.setCurrentPrice(priceData.getPrice());
            }
        } catch (Exception e) {
            // 查詢失敗不影響警報資料回傳
            // currentPrice 保持為 null
        }

        return response;
    }
}