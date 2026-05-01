package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AssetRequest;
import com.pocketfolio.backend.dto.AssetResponse;
import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.Asset;
import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.entity.Transaction;
import com.pocketfolio.backend.entity.TransactionType;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
import com.pocketfolio.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // ── Create ──────────────────────────────────────────
    @Transactional
    public AssetResponse createAsset(AssetRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 確認帳戶存在且屬於當前用戶
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + request.getAccountId() + " 的帳戶"));

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("無權使用此帳戶");
        }

        // 檢查同一帳戶是否已有相同代號的資產
        if (assetRepository.existsByUserIdAndAccountIdAndSymbol(
                currentUserId, request.getAccountId(), request.getSymbol())) {
            throw new IllegalArgumentException(
                    "帳戶「" + account.getName() + "」已有代號為「" + request.getSymbol() + "」的資產");
        }

        Asset asset = new Asset();
        asset.setAccount(account);
        asset.setType(request.getType());
        asset.setSymbol(request.getSymbol().toUpperCase());
        asset.setName(request.getName());
        asset.setQuantity(request.getQuantity());
        asset.setCostPrice(request.getCostPrice());
        asset.setCurrentPrice(request.getCostPrice());  // 初始時用成本價
        asset.setPriceCurrency(request.getType() == AssetType.CRYPTO ? "USD" : "TWD");
        asset.setNote(request.getNote());

        // 設定用戶關聯
        User user = new User();
        user.setId(currentUserId);
        asset.setUser(user);

        Asset saved = assetRepository.save(asset);

        // 若有填寫來源帳戶，自動建立轉帳記錄（TRANSFER_OUT → TRANSFER_IN）
        if (request.getFromAccountId() != null) {
            Account fromAccount = accountRepository.findById(request.getFromAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到 ID 為 " + request.getFromAccountId() + " 的來源帳戶"));
            if (!fromAccount.getUser().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("無權使用此帳戶");
            }
            if (fromAccount.getId().equals(account.getId())) {
                throw new IllegalArgumentException("來源帳戶與投資帳戶不能相同");
            }

            BigDecimal totalCost = request.getQuantity().multiply(request.getCostPrice());
            UUID groupId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            String note = "購入 " + saved.getName();

            Transaction out = new Transaction();
            out.setType(TransactionType.TRANSFER_OUT);
            out.setAmount(totalCost);
            out.setNote(note);
            out.setDate(today);
            out.setAccount(fromAccount);
            out.setTransferGroupId(groupId);
            out.setUser(user);

            Transaction in = new Transaction();
            in.setType(TransactionType.TRANSFER_IN);
            in.setAmount(totalCost);
            in.setNote(note);
            in.setDate(today);
            in.setAccount(account);
            in.setTransferGroupId(groupId);
            in.setUser(user);

            transactionRepository.save(out);
            transactionRepository.save(in);
        }

        return toResponse(saved);
    }

    // ── Read (單筆) ───────────────────────────────────────
    public AssetResponse getAsset(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的資產"));

        // 驗證資產屬於當前用戶
        if (!asset.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的資產");
        }

        return toResponse(asset);
    }

    // ── Read (某帳戶的所有資產) ────────────────────────────
    public List<AssetResponse> getAssetsByAccount(UUID accountId) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 確認帳戶存在且屬於當前用戶
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到帳戶"));

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到帳戶");
        }

        return assetRepository.findByUserIdAndAccountId(currentUserId, accountId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Read (某帳戶的特定類型資產) ────────────────────────
    public List<AssetResponse> getAssetsByAccountAndType(UUID accountId, AssetType type) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 確認帳戶存在且屬於當前用戶
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到帳戶"));

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到帳戶");
        }

        return assetRepository.findByUserIdAndAccountIdAndType(currentUserId, accountId, type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update ───────────────────────────────────────────
    public AssetResponse updateAsset(UUID id, AssetRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的資產"));

        // 驗證資產屬於當前用戶
        if (!asset.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的資產");
        }

        // 如果改了代號，檢查新代號是否重複
        if (!asset.getSymbol().equals(request.getSymbol().toUpperCase())
                && assetRepository.existsByUserIdAndAccountIdAndSymbol(
                currentUserId, asset.getAccount().getId(), request.getSymbol())) {
            throw new IllegalArgumentException("此帳戶已有代號為「" + request.getSymbol() + "」的資產");
        }

        asset.setType(request.getType());
        asset.setSymbol(request.getSymbol().toUpperCase());
        asset.setName(request.getName());
        asset.setQuantity(request.getQuantity());
        asset.setCostPrice(request.getCostPrice());
        asset.setNote(request.getNote());

        return toResponse(assetRepository.save(asset));
    }

    // ── Delete ───────────────────────────────────────────
    public void deleteAsset(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的資產"));

        // 驗證資產屬於當前用戶
        if (!asset.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的資產");
        }

        assetRepository.deleteById(id);
    }

    // ── Helper ───────────────────────────────────────────
    private AssetResponse toResponse(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getId())
                .accountId(asset.getAccount().getId())
                .accountName(asset.getAccount().getName())
                .type(asset.getType())
                .symbol(asset.getSymbol())
                .name(asset.getName())
                .quantity(asset.getQuantity())
                .costPrice(asset.getCostPrice())
                .currentPrice(asset.getCurrentPrice())
                .lastPriceUpdate(asset.getLastPriceUpdate())
                .marketValue(asset.getMarketValue())
                .profitLoss(asset.getProfitLoss())
                .profitLossPercent(asset.getProfitLossPercent())
                .priceCurrency(asset.getPriceCurrency())
                .note(asset.getNote())
                .build();
    }
}