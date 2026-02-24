package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AccountRequest;
import com.pocketfolio.backend.dto.AccountResponse;
import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.AccountType;
import com.pocketfolio.backend.entity.Asset;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    // ── Create ──────────────────────────────────────────
    public AccountResponse createAccount(AccountRequest request) {
        // 檢查名稱是否重複
        if (repository.existsByName(request.getName())) {
            throw new IllegalArgumentException("帳戶名稱「" + request.getName() + "」已存在");
        }

        Account account = new Account();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setDescription(request.getDescription());
        account.setCurrency(request.getCurrency());

        return toResponse(repository.save(account));
    }

    // ── Read (單筆) ───────────────────────────────────────
    public AccountResponse getAccount(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的帳戶"));
        return toResponse(account);
    }

    // ── Read (所有) ───────────────────────────────────────
    public List<AccountResponse> getAllAccounts() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Read (依類型) ─────────────────────────────────────
    public List<AccountResponse> getAccountsByType(AccountType type) {
        return repository.findByType(type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Read (搜尋) ───────────────────────────────────────
    public List<AccountResponse> searchAccounts(String keyword) {
        return repository.findByNameContainingIgnoreCase(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update ───────────────────────────────────────────
    public AccountResponse updateAccount(UUID id, AccountRequest request) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到 ID 為 " + id + " 的帳戶"));

        // 如果改了名稱，檢查新名稱是否與其他帳戶重複
        if (!account.getName().equals(request.getName())
                && repository.existsByName(request.getName())) {
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
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("找不到 ID 為 " + id + " 的帳戶");
        }
        // TODO Phase 2: 檢查是否有關聯的交易，若有則不能刪除
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
    // Phase 2 最後會加入交易計算邏輯
    private BigDecimal calculateCurrentBalance(Account account) {
        // 投資帳戶：餘額 = 所有資產的市值加總
        if (account.getType() == AccountType.INVESTMENT) {
            return account.getAssets().stream()
                    .map(Asset::getMarketValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 其他帳戶：目前只回傳 initialBalance
        // Phase 2 最後會改成：initialBalance + sum(transactions)
        return account.getInitialBalance();
    }
}