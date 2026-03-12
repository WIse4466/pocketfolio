package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AccountBalanceResponse;
import com.pocketfolio.backend.dto.AccountRequest;
import com.pocketfolio.backend.dto.AccountResponse;
import com.pocketfolio.backend.entity.*;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.TransactionRepository;
import com.pocketfolio.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final TransactionRepository transactionRepository;

    // ── Create ──────────────────────────────────────────
    public AccountResponse createAccount(AccountRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 檢查當前用戶是否已有同名帳戶
        if (repository.existsByUserIdAndName(currentUserId, request.getName())) {
            throw new IllegalArgumentException("帳戶名稱「" + request.getName() + "」已存在");
        }

        Account account = new Account();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setDescription(request.getDescription());
        account.setCurrency(request.getCurrency());

        // 設定用戶關聯
        User user = new User();
        user.setId(currentUserId);
        account.setUser(user);

        return toResponse(repository.save(account));
    }

    // ── Read (單筆) ───────────────────────────────────────
    public AccountResponse getAccount(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Account account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的帳戶"));

        // 驗證帳戶屬於當前用戶
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的帳戶");
        }

        return toResponse(account);
    }

    // ── Read (所有) ───────────────────────────────────────
    public List<AccountResponse> getAllAccounts() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return repository.findByUserId(currentUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Read (依類型) ─────────────────────────────────────
    public List<AccountResponse> getAccountsByType(AccountType type) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return repository.findByUserIdAndType(currentUserId, type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Read (搜尋) ───────────────────────────────────────
    public List<AccountResponse> searchAccounts(String keyword) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return repository.findByUserIdAndNameContainingIgnoreCase(currentUserId, keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update ───────────────────────────────────────────
    public AccountResponse updateAccount(UUID id, AccountRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Account account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的帳戶"));

        // 驗證帳戶屬於當前用戶
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的帳戶");
        }

        // 如果改了名稱，檢查新名稱是否與當前用戶的其他帳戶重複
        if (!account.getName().equals(request.getName())
                && repository.existsByUserIdAndName(currentUserId, request.getName())) {
            throw new IllegalArgumentException("帳戶名稱「" + request.getName() + "」已存在");
        }

        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setDescription(request.getDescription());
        account.setCurrency(request.getCurrency());

        return toResponse(repository.save(account));
    }

    // ── Delete ───────────────────────────────────────────
    public void deleteAccount(UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        Account account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的帳戶"));

        // 驗證帳戶屬於當前用戶
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的帳戶");
        }

        repository.deleteById(id);
    }

    // ── Helper ───────────────────────────────────────────
    private AccountResponse toResponse(Account account) {
        // 目前 currentBalance 直接等於 initialBalance
        // Phase 2 最後會改成：initialBalance + sum(transactions)
        BigDecimal currentBalance = calculateCurrentBalance(account);

        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .initialBalance(account.getInitialBalance())
                .currentBalance(currentBalance)
                .description(account.getDescription())
                .currency(account.getCurrency())
                .build();
    }

    // 計算當前餘額（目前只是回傳初始餘額）
    private BigDecimal calculateCurrentBalance(Account account) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        // 投資帳戶：餘額 = 所有資產的市值加總
        if (account.getType() == AccountType.INVESTMENT) {
            return account.getAssets().stream()
                    .filter(asset -> asset.getUser().getId().equals(currentUserId))
                    .map(Asset::getMarketValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 其他帳戶：使用 Repository 的計算方法
        BigDecimal netAmount = transactionRepository.calculateNetAmountByAccountIdAndUserId(
                account.getId(), currentUserId);

        return account.getInitialBalance().add(netAmount);
    }

    // 取得所有帳戶餘額（含變動資訊）
    public List<AccountBalanceResponse> getAllAccountBalances() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();

        return repository.findByUserId(currentUserId).stream()
                .map(this::toBalanceResponse)
                .collect(Collectors.toList());
    }

    private AccountBalanceResponse toBalanceResponse(Account account) {
        BigDecimal currentBalance = calculateCurrentBalance(account);
        BigDecimal change = currentBalance.subtract(account.getInitialBalance());

        BigDecimal changePercent = account.getInitialBalance().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : change.divide(account.getInitialBalance(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);

        return AccountBalanceResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .initialBalance(account.getInitialBalance())
                .currentBalance(currentBalance)
                .change(change)
                .changePercent(changePercent)
                .build();
    }
}