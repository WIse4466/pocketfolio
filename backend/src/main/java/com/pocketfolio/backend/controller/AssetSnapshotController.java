package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AssetHistoryResponse;
import com.pocketfolio.backend.dto.AssetSnapshotResponse;
import com.pocketfolio.backend.dto.PortfolioSnapshotResponse;
import com.pocketfolio.backend.service.AssetSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
@Tag(name = "10. 資產快照", description = "歷史快照與趨勢分析")
@SecurityRequirement(name = "bearerAuth")
public class AssetSnapshotController {

    private final AssetSnapshotService service;

    @PostMapping("/asset/{assetId}")
    @Operation(
            summary = "手動建立資產快照",
            description = "立即為指定資產建立價格快照（每天只能建立一次）"
    )
    public ResponseEntity<AssetSnapshotResponse> createSnapshot(@PathVariable UUID assetId) {
        AssetSnapshotResponse response = service.createSnapshot(assetId);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();  // 今天已有快照
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/asset/{assetId}/history")
    @Operation(
            summary = "查詢資產歷史走勢",
            description = "取得指定資產在一段時間內的價格與損益變化（用於繪製圖表）"
    )
    public ResponseEntity<AssetHistoryResponse> getAssetHistory(
            @PathVariable UUID assetId,

            @Parameter(description = "查詢天數（預設 30 天）")
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        return ResponseEntity.ok(service.getAssetHistory(assetId, days));
    }

    @GetMapping("/portfolio/{date}")
    @Operation(
            summary = "查詢特定日期的投資組合快照",
            description = "取得指定日期的所有資產狀態與總市值"
    )
    public ResponseEntity<PortfolioSnapshotResponse> getPortfolioSnapshot(
            @Parameter(description = "日期（格式：YYYY-MM-DD）")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        PortfolioSnapshotResponse response = service.getPortfolioSnapshot(date);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/portfolio/history")
    @Operation(
            summary = "查詢投資組合歷史趨勢",
            description = "取得一段時間內的總資產變化（用於繪製資產走勢圖）"
    )
    public ResponseEntity<List<PortfolioSnapshotResponse>> getPortfolioHistory(
            @Parameter(description = "查詢天數（預設 30 天）")
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        return ResponseEntity.ok(service.getPortfolioHistory(days));
    }
}