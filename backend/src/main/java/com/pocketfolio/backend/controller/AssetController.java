package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AssetRequest;
import com.pocketfolio.backend.dto.AssetResponse;
import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.service.AssetService;
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
public class AssetController {

    private final AssetService service;

    @PostMapping
    public ResponseEntity<AssetResponse> create(
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAsset(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAsset(id));
    }

    // 查詢某帳戶的所有資產
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AssetResponse>> getByAccount(
            @PathVariable UUID accountId,
            @RequestParam(required = false) AssetType type) {

        if (type != null) {
            return ResponseEntity.ok(service.getAssetsByAccountAndType(accountId, type));
        }
        return ResponseEntity.ok(service.getAssetsByAccount(accountId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(service.updateAsset(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}