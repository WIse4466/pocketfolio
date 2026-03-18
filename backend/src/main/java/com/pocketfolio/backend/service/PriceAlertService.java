package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.PriceAlertRequest;
import com.pocketfolio.backend.dto.PriceAlertResponse;
import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.entity.Asset;
import com.pocketfolio.backend.entity.PriceAlert;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.PriceAlertRepository;
import com.pocketfolio.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {

    private final PriceAlertRepository alertRepository;
    private final AssetRepository assetRepository;

    // ── Create ──────────────────────────────────────────
    public PriceAlertResponse createAlert(PriceAlertRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        PriceAlert alert = new PriceAlert();
        alert.setSymbol(request.getSymbol().toUpperCase());
        alert.setAssetType(request.getAssetType());
        alert.setCondition(request.getCondition());
        alert.setTargetPrice(request.getTargetPrice());
        alert.setNote(request.getNote());
        alert.setActive(true);
        alert.setTriggered(false);

        // 設定用戶關聯
        User user = new User();
        user.setId(currentUserId);
        alert.setUser(user);

        // 如果有指定資產，綁定資產
        if (request.getAssetId() != null) {
            Asset asset = assetRepository.findById(request.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("找不到資產"));

            // 驗證資產屬於當前用戶
            if (!asset.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此資產");
            }

            alert.setAsset(asset);
        }

        PriceAlert saved = alertRepository.save(alert);

        log.info("價格警報已建立: {} {} ${}",
                saved.getSymbol(),
                saved.getCondition(),
                saved.getTargetPrice());

        return toResponse(saved, null);
    }

    // ── Read (單筆) ───────────────────────────────────────
    public PriceAlertResponse getAlert(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到警報"));

        // 驗證警報屬於當前用戶
        if (!alert.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("無權使用此資產");
        }

        return toResponse(alert, null);
    }

    // ── Read (用戶的所有警報) ─────────────────────────────
    public List<PriceAlertResponse> getUserAlerts() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return alertRepository.findByUserId(currentUserId).stream()
                .map(alert -> toResponse(alert, null))
                .collect(Collectors.toList());
    }

    // ── Read (用戶的啟用警報) ─────────────────────────────
    public List<PriceAlertResponse> getUserActiveAlerts() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return alertRepository.findByUserIdAndActiveTrue(currentUserId).stream()
                .map(alert -> toResponse(alert, null))
                .collect(Collectors.toList());
    }

    // ── Update ───────────────────────────────────────────
    public PriceAlertResponse updateAlert(UUID id, PriceAlertRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到警報"));

        // 驗證警報屬於當前用戶
        if (!alert.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("無權使用此資產");
        }

        alert.setSymbol(request.getSymbol().toUpperCase());
        alert.setAssetType(request.getAssetType());
        alert.setCondition(request.getCondition());
        alert.setTargetPrice(request.getTargetPrice());
        alert.setNote(request.getNote());

        // 更新時重置觸發狀態
        alert.setTriggered(false);
        alert.setTriggeredAt(null);

        return toResponse(alertRepository.save(alert), null);
    }

    // ── Delete ───────────────────────────────────────────
    public void deleteAlert(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到警報"));

        // 驗證警報屬於當前用戶
        if (!alert.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("無權使用此資產");
        }

        alertRepository.deleteById(id);
        log.info("價格警報已刪除: {}", alert.getSymbol());
    }

    // ── 啟用/停用警報 ──────────────────────────────────────
    public PriceAlertResponse toggleAlert(UUID id, boolean active) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到警報"));

        // 驗證警報屬於當前用戶
        if (!alert.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("無權使用此資產");
        }

        alert.setActive(active);

        if (active) {
            // 重新啟用時重置觸發狀態
            alert.setTriggered(false);
            alert.setTriggeredAt(null);
        }

        return toResponse(alertRepository.save(alert), null);
    }

    // ── 檢查價格警報（核心邏輯）───────────────────────────
    public List<PriceAlert> checkPriceAlerts(String symbol, BigDecimal currentPrice) {
        List<PriceAlert> alerts = alertRepository.findActiveAlertsBySymbol(symbol);
        List<PriceAlert> triggeredAlerts = alerts.stream()
                .filter(alert -> shouldTrigger(alert, currentPrice))
                .collect(Collectors.toList());

        // 標記為已觸發
        triggeredAlerts.forEach(alert -> {
            alert.setTriggered(true);
            alert.setTriggeredAt(LocalDateTime.now());
            alertRepository.save(alert);

            log.info("警報已觸發: {} {} ${} (當前價格: ${})",
                    alert.getSymbol(),
                    alert.getCondition(),
                    alert.getTargetPrice(),
                    currentPrice);
        });

        return triggeredAlerts;
    }

    // ── Helper: 判斷是否應該觸發 ────────────────────────────
    private boolean shouldTrigger(PriceAlert alert, BigDecimal currentPrice) {
        if (alert.getCondition() == PriceAlert.AlertCondition.ABOVE) {
            // 當前價格 >= 目標價格
            return currentPrice.compareTo(alert.getTargetPrice()) >= 0;
        } else {
            // 當前價格 <= 目標價格
            return currentPrice.compareTo(alert.getTargetPrice()) <= 0;
        }
    }

    // ── Helper: Entity → DTO ─────────────────────────────
    private PriceAlertResponse toResponse(PriceAlert alert, BigDecimal currentPrice) {

        // 生成條件描述
        String conditionText = String.format("當價格%s $%s",
                alert.getCondition() == PriceAlert.AlertCondition.ABOVE ? "高於" : "低於",
                alert.getTargetPrice());

        return PriceAlertResponse.builder()
                .id(alert.getId())
                .assetId(alert.getAsset() != null ? alert.getAsset().getId() : null)
                .symbol(alert.getSymbol())
                .assetType(alert.getAssetType())
                .condition(alert.getCondition())
                .targetPrice(alert.getTargetPrice())
                .active(alert.isActive())
                .triggered(alert.isTriggered())
                .triggeredAt(alert.getTriggeredAt())
                .createdAt(alert.getCreatedAt())
                .note(alert.getNote())
                .currentPrice(currentPrice)
                .conditionText(conditionText)
                .build();
    }
}