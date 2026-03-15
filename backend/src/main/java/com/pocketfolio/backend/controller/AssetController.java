package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AssetRequest;
import com.pocketfolio.backend.dto.AssetResponse;
import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.service.AssetService;
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
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "5. 資產管理", description = "管理投資資產（股票、加密貨幣、基金）")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService service;

    @PostMapping
    @Operation(summary = "建立資產", description = "在投資帳戶中新增一項資產")
    public ResponseEntity<AssetResponse> create(
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAsset(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢單個資產", description = "根據 ID 查詢資產詳情，包含損益計算")
    public ResponseEntity<AssetResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAsset(id));
    }

    // 查詢某帳戶的所有資產
    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "查詢帳戶的所有資產",
            description = "查詢指定投資帳戶下的所有資產，可依類型篩選"
    )
    public ResponseEntity<List<AssetResponse>> getByAccount(
            @PathVariable UUID accountId,
            @RequestParam(required = false) AssetType type) {

        if (type != null) {
            return ResponseEntity.ok(service.getAssetsByAccountAndType(accountId, type));
        }
        return ResponseEntity.ok(service.getAssetsByAccount(accountId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新資產", description = "修改指定資產的內容")
    public ResponseEntity<AssetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(service.updateAsset(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除資產", description = "刪除指定的資產")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}