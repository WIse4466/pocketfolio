package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AssetHistoryResponse;
import com.pocketfolio.backend.dto.AssetSnapshotResponse;
import com.pocketfolio.backend.dto.PortfolioSnapshotResponse;
import com.pocketfolio.backend.entity.Asset;
import com.pocketfolio.backend.entity.AssetSnapshot;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.AssetSnapshotRepository;
import com.pocketfolio.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetSnapshotService {

    private final AssetSnapshotRepository snapshotRepository;
    private final AssetRepository assetRepository;

    // ── 建立單一資產的快照 ──────────────────────────────────
    public AssetSnapshotResponse createSnapshot(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("找不到資產"));

        // 檢查今天是否已有快照
        LocalDate today = LocalDate.now();
        if (snapshotRepository.existsByAssetIdAndSnapshotDate(assetId, today)) {
            log.info("資產 {} 今天已有快照，跳過", asset.getSymbol());
            return null;
        }

        AssetSnapshot snapshot = AssetSnapshot.fromAsset(asset);
        AssetSnapshot saved = snapshotRepository.save(snapshot);

        log.info("資產快照已建立: {} (${}) - {}",
                asset.getSymbol(),
                asset.getCurrentPrice(),
                today);

        return toResponse(saved);
    }

    // ── 建立所有資產的快照（定時任務用）────────────────────────
    public int createAllSnapshots() {
        List<Asset> allAssets = assetRepository.findAll();
        int count = 0;
        LocalDate today = LocalDate.now();

        log.info("開始建立資產快照，共 {} 筆資產", allAssets.size());

        for (Asset asset : allAssets) {
            try {
                // 檢查是否已有快照
                if (!snapshotRepository.existsByAssetIdAndSnapshotDate(asset.getId(), today)) {
                    AssetSnapshot snapshot = AssetSnapshot.fromAsset(asset);
                    snapshotRepository.save(snapshot);
                    count++;
                }
            } catch (Exception e) {
                log.error("建立快照失敗: {} - {}", asset.getSymbol(), e.getMessage());
            }
        }

        log.info("快照建立完成：{}/{} 成功", count, allAssets.size());
        return count;
    }

    // ── 查詢資產的歷史快照 ──────────────────────────────────
    public AssetHistoryResponse getAssetHistory(UUID assetId, Integer days) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("找不到資產"));

        // 驗證資產屬於當前用戶
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        if (!asset.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("無權查詢此資產");
        }

        // 計算日期範圍
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 30);

        List<AssetSnapshot> snapshots = snapshotRepository
                .findByAssetIdAndSnapshotDateBetweenOrderBySnapshotDate(
                        assetId, startDate, endDate);

        // 轉換為圖表資料點
        List<AssetHistoryResponse.DataPoint> dataPoints = snapshots.stream()
                .map(snapshot -> AssetHistoryResponse.DataPoint.builder()
                        .date(snapshot.getSnapshotDate().toString())
                        .price(snapshot.getCurrentPrice().doubleValue())
                        .marketValue(snapshot.getMarketValue().doubleValue())
                        .profitLoss(snapshot.getProfitLoss().doubleValue())
                        .profitLossPercent(snapshot.getProfitLossPercent().doubleValue())
                        .build())
                .collect(Collectors.toList());

        return AssetHistoryResponse.builder()
                .symbol(asset.getSymbol())
                .assetName(asset.getName())
                .history(dataPoints)
                .build();
    }

    // ── 查詢用戶在特定日期的投資組合快照 ────────────────────
    public PortfolioSnapshotResponse getPortfolioSnapshot(LocalDate date) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        List<AssetSnapshot> snapshots = snapshotRepository
                .findByUserIdAndSnapshotDate(currentUserId, date);

        if (snapshots.isEmpty()) {
            return null;
        }

        // 計算總計
        BigDecimal totalMarketValue = snapshots.stream()
                .map(AssetSnapshot::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = snapshots.stream()
                .map(s -> s.getCostPrice().multiply(s.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLoss = totalMarketValue.subtract(totalCost);

        BigDecimal totalProfitLossPercent = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        List<AssetSnapshotResponse> assetList = snapshots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PortfolioSnapshotResponse.builder()
                .date(date)
                .totalMarketValue(totalMarketValue)
                .totalCost(totalCost)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(totalProfitLossPercent)
                .assetCount(snapshots.size())
                .assets(assetList)
                .build();
    }

    // ── 查詢用戶的投資組合歷史趨勢 ──────────────────────────
    public List<PortfolioSnapshotResponse> getPortfolioHistory(Integer days) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 30);

        List<AssetSnapshot> allSnapshots = snapshotRepository
                .findByUserIdAndSnapshotDateBetweenOrderBySnapshotDate(
                        currentUserId, startDate, endDate);

        // 按日期分組
        return allSnapshots.stream()
                .collect(Collectors.groupingBy(AssetSnapshot::getSnapshotDate))
                .entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<AssetSnapshot> dailySnapshots = entry.getValue();

                    BigDecimal totalMarketValue = dailySnapshots.stream()
                            .map(AssetSnapshot::getMarketValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalCost = dailySnapshots.stream()
                            .map(s -> s.getCostPrice().multiply(s.getQuantity()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalProfitLoss = totalMarketValue.subtract(totalCost);

                    BigDecimal totalProfitLossPercent = totalCost.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);

                    return PortfolioSnapshotResponse.builder()
                            .date(date)
                            .totalMarketValue(totalMarketValue)
                            .totalCost(totalCost)
                            .totalProfitLoss(totalProfitLoss)
                            .totalProfitLossPercent(totalProfitLossPercent)
                            .assetCount(dailySnapshots.size())
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    // ── Helper: Entity → DTO ─────────────────────────────
    private AssetSnapshotResponse toResponse(AssetSnapshot snapshot) {
        return AssetSnapshotResponse.builder()
                .id(snapshot.getId())
                .assetId(snapshot.getAsset().getId())
                .symbol(snapshot.getSymbol())
                .assetName(snapshot.getAssetName())
                .assetType(snapshot.getAssetType())
                .quantity(snapshot.getQuantity())
                .costPrice(snapshot.getCostPrice())
                .currentPrice(snapshot.getCurrentPrice())
                .marketValue(snapshot.getMarketValue())
                .profitLoss(snapshot.getProfitLoss())
                .profitLossPercent(snapshot.getProfitLossPercent())
                .snapshotDate(snapshot.getSnapshotDate())
                .build();
    }
}