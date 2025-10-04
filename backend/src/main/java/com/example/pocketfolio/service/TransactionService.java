package com.example.pocketfolio.service;

import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.entity.*;
import com.example.pocketfolio.exception.NotFoundException;
import com.example.pocketfolio.exception.BusinessException;
import com.example.pocketfolio.exception.ErrorCode;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.CategoryRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest req) {
        if (req.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (req.getKind() == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (req.getOccurredAt() == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (req.getCurrencyCode() == null || req.getCurrencyCode().length() != 3) {
            throw new IllegalArgumentException("currencyCode (3-letter) is required");
        }

        Transaction tx = new Transaction();
        tx.setUserId(req.getUserId());
        tx.setKind(req.getKind());
        tx.setAmount(req.getAmount());
        tx.setOccurredAt(req.getOccurredAt());
        tx.setNotes(req.getNotes());
        tx.setCurrencyCode(req.getCurrencyCode());
        tx.setFxRateUsed(req.getFxRateUsed());

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found: " + req.getCategoryId()));
            tx.setCategory(category);
        }

        switch (req.getKind()) {
            case INCOME -> handleIncomeExpense(tx, req.getAccountId(), true);
            case EXPENSE -> handleIncomeExpense(tx, req.getAccountId(), false);
            case TRANSFER -> handleTransfer(tx, req.getSourceAccountId(), req.getTargetAccountId());
        }

        return transactionRepository.save(tx);
    }

    private void handleIncomeExpense(Transaction tx, UUID accountId, boolean isIncome) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required for income/expense");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        if (account.isArchived()) {
            throw new IllegalArgumentException("Account is archived and cannot accept transactions");
        }
        ensureSameCurrency(tx.getCurrencyCode(), account.getCurrencyCode());

        BigDecimal amount = tx.getAmount();
        BigDecimal newBalance = account.getCurrentBalance();
        newBalance = isIncome ? newBalance.add(amount) : newBalance.subtract(amount);
        account.setCurrentBalance(newBalance);

        tx.setAccount(account);
        tx.setSourceAccount(null);
        tx.setTargetAccount(null);
    }

    private void handleTransfer(Transaction tx, UUID sourceId, UUID targetId) {
        if (sourceId == null || targetId == null) {
            throw new IllegalArgumentException("sourceAccountId and targetAccountId are required for transfer");
        }
        if (Objects.equals(sourceId, targetId)) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT, "來源帳戶與目標帳戶不可相同");
        }

        Account source = accountRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Source account not found: " + sourceId));
        Account target = accountRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Target account not found: " + targetId));

        if (source.isArchived() || target.isArchived()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ARCHIVED, "Source/Target account is archived and cannot accept transactions");
        }

        // Credit card specific rules
        boolean bothCreditCard = source.getType() == AccountType.CREDIT_CARD && target.getType() == AccountType.CREDIT_CARD;
        if (bothCreditCard) {
            throw new BusinessException(ErrorCode.TRANSFER_PAIR_INVALID, "Transfers between credit cards are not supported.");
        }
        if (source.getType() == AccountType.CREDIT_CARD) {
            throw new BusinessException(ErrorCode.TRANSFER_DIRECTION_INVALID, "Transfers from a credit card are not supported.");
        }

        // MVP: only same currency transfers and tx currency must match accounts
        ensureSameCurrency(tx.getCurrencyCode(), source.getCurrencyCode());
        if (source.getCurrencyCode() == null || target.getCurrencyCode() == null || !source.getCurrencyCode().equalsIgnoreCase(target.getCurrencyCode())) {
            throw new BusinessException(ErrorCode.CROSS_CURRENCY_UNSUPPORTED, "Cross-currency transfers are not supported.");
        }

        BigDecimal amount = tx.getAmount();

        source.setCurrentBalance(source.getCurrentBalance().subtract(amount));
        target.setCurrentBalance(target.getCurrentBalance().add(amount));

        tx.setAccount(null);
        tx.setSourceAccount(source);
        tx.setTargetAccount(target);
        tx.setCategory(null); // category ignored for transfer in MVP
    }

    private void ensureSameCurrency(String a, String b) {
        if (a == null || b == null || !a.equalsIgnoreCase(b)) {
            throw new BusinessException(ErrorCode.CROSS_CURRENCY_UNSUPPORTED, "跨幣別交易尚未支援（MVP 限單一幣別）");
        }
    }

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(UUID userId, Instant from, Instant to, Pageable pageable) {
        UUID uid = (userId != null) ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant start = (from != null) ? from : Instant.EPOCH;
        Instant end = (to != null) ? to : Instant.now();
        return transactionRepository.findByUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(uid, start, end, pageable);
    }

    @Transactional
    public void deleteTransaction(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        switch (tx.getKind()) {
            case INCOME -> rollbackIncomeExpense(tx, true);
            case EXPENSE -> rollbackIncomeExpense(tx, false);
            case TRANSFER -> rollbackTransfer(tx);
        }

        transactionRepository.delete(tx);
    }

    private void rollbackIncomeExpense(Transaction tx, boolean wasIncome) {
        Account account = tx.getAccount();
        if (account == null) {
            throw new IllegalStateException("Transaction missing account reference");
        }
        BigDecimal amount = tx.getAmount();
        // Reverse of create: income added -> subtract; expense subtracted -> add
        BigDecimal newBalance = account.getCurrentBalance();
        newBalance = wasIncome ? newBalance.subtract(amount) : newBalance.add(amount);
        account.setCurrentBalance(newBalance);
    }

    private void rollbackTransfer(Transaction tx) {
        Account source = tx.getSourceAccount();
        Account target = tx.getTargetAccount();
        if (source == null || target == null) {
            throw new IllegalStateException("Transfer transaction missing accounts");
        }
        BigDecimal amount = tx.getAmount();
        // Reverse of create: source -= amount; target += amount
        source.setCurrentBalance(source.getCurrentBalance().add(amount));
        target.setCurrentBalance(target.getCurrentBalance().subtract(amount));
    }
}
