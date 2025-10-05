package com.example.pocketfolio.service;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.AccountType;
import com.example.pocketfolio.exception.BusinessException;
import com.example.pocketfolio.exception.ErrorCode;
import com.example.pocketfolio.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(Account account) {
        normalizeDueFields(account);
        validateDueFieldsOnType(account.getType(), account.getDueMonthOffset(), account.getDueHolidayPolicy(), account.isAutopayEnabled(), account.getAutopayAccount());
        // Basic validation for autopayAccount to prevent circular references
        if (account.getAutopayAccount() != null) {
            Account autopayAccount = accountRepository.findById(account.getAutopayAccount().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Autopay account not found."));

            // Prevent an account from autopaying itself
            if (Objects.equals(account.getId(), autopayAccount.getId())) {
                throw new IllegalArgumentException("An account cannot autopay itself.");
            }
            // Prevent circular autopay (e.g., A -> B, B -> A)
            if (autopayAccount.getAutopayAccount() != null && Objects.equals(autopayAccount.getAutopayAccount().getId(), account.getId())) {
                throw new IllegalArgumentException("Circular autopay relationship detected.");
            }
            if (autopayAccount.getType() == AccountType.CREDIT_CARD) {
                throw new BusinessException(ErrorCode.AUTOPAY_ACCOUNT_INVALID, "Autopay account cannot be a credit card.");
            }
            account.setAutopayAccount(autopayAccount);
            account.setAutopayEnabled(true);
        }

        // Set current balance to initial balance on creation
        account.setCurrentBalance(account.getInitialBalance());

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));
    }

    @Transactional
    public Account updateAccount(UUID id, Account updatedAccount) {
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));

        // Update fields
        existingAccount.setName(updatedAccount.getName());
        existingAccount.setType(updatedAccount.getType());
        existingAccount.setCurrencyCode(updatedAccount.getCurrencyCode());
        // initialBalance is typically not updated after creation, but if needed, add logic here
        // existingAccount.setInitialBalance(updatedAccount.getInitialBalance());
        // currentBalance is updated by transactions, not directly via this method
        // existingAccount.setCurrentBalance(updatedAccount.getCurrentBalance());
        existingAccount.setIncludeInNetWorth(updatedAccount.isIncludeInNetWorth());
        existingAccount.setArchived(updatedAccount.isArchived());
        existingAccount.setClosingDay(updatedAccount.getClosingDay());
        existingAccount.setDueDay(updatedAccount.getDueDay());
        existingAccount.setNotes(updatedAccount.getNotes());

        // Normalize & validate due fields
        // Only meaningful for credit cards but we store defaults for consistency.
        short offset = normalizedOffset(updatedAccount.getDueMonthOffset());
        String policy = normalizedPolicy(updatedAccount.getDueHolidayPolicy());
        boolean autopayEnabled = updatedAccount.isAutopayEnabled();
        validateDueFieldsOnType(existingAccount.getType(), offset, policy, autopayEnabled, updatedAccount.getAutopayAccount());
        existingAccount.setDueMonthOffset(offset);
        existingAccount.setDueHolidayPolicy(policy);
        existingAccount.setAutopayEnabled(autopayEnabled);

        // Handle autopayAccount update with validation
        if (updatedAccount.getAutopayAccount() != null) {
            Account newAutopayAccount = accountRepository.findById(updatedAccount.getAutopayAccount().getId())
                    .orElseThrow(() -> new IllegalArgumentException("New autopay account not found."));
            if (Objects.equals(existingAccount.getId(), newAutopayAccount.getId())) {
                throw new IllegalArgumentException("An account cannot autopay itself.");
            }
            if (newAutopayAccount.getAutopayAccount() != null && Objects.equals(newAutopayAccount.getAutopayAccount().getId(), existingAccount.getId())) {
                throw new IllegalArgumentException("Circular autopay relationship detected.");
            }
            if (newAutopayAccount.getType() == AccountType.CREDIT_CARD) {
                throw new BusinessException(ErrorCode.AUTOPAY_ACCOUNT_INVALID, "Autopay account cannot be a credit card.");
            }
            existingAccount.setAutopayAccount(newAutopayAccount);
            existingAccount.setAutopayEnabled(true);
        } else {
            // If explicitly disabled, clear autopay target as well
            if (!autopayEnabled) {
                existingAccount.setAutopayAccount(null);
            }
        }

        return accountRepository.save(existingAccount);
    }

    @Transactional
    public void deleteAccount(UUID id) {
        if (!accountRepository.existsById(id)) {
            throw new IllegalArgumentException("Account not found with id: " + id);
        }
        // TODO: Add logic to handle transactions associated with this account before deletion
        accountRepository.deleteById(id);
    }

    private void normalizeDueFields(Account account) {
        account.setDueMonthOffset(normalizedOffset(account.getDueMonthOffset()));
        account.setDueHolidayPolicy(normalizedPolicy(account.getDueHolidayPolicy()));
        // autopayEnabled remains as-is; it will be set true if autopayAccount is provided
    }

    private short normalizedOffset(Short v) {
        return v == null ? (short)1 : v;
    }

    private String normalizedPolicy(String v) {
        return (v == null || v.isBlank()) ? "NONE" : v;
    }

    private void validateDueFieldsOnType(AccountType type, short offset, String policy, boolean autopayEnabled, Account autopayAccount) {
        if (offset < 0 || offset > 2) {
            throw new BusinessException(ErrorCode.DUE_MONTH_OFFSET_INVALID, "dueMonthOffset must be 0,1,2");
        }
        if (!Objects.equals(policy, "NONE") && !Objects.equals(policy, "ADVANCE") && !Objects.equals(policy, "POSTPONE")) {
            throw new BusinessException(ErrorCode.DUE_HOLIDAY_POLICY_INVALID, "dueHolidayPolicy must be NONE|ADVANCE|POSTPONE");
        }
        if (type != AccountType.CREDIT_CARD) {
            if (autopayEnabled) {
                throw new BusinessException(ErrorCode.AUTOPAY_NOT_SUPPORTED, "Autopay is supported only for credit cards.");
            }
        }
        if (autopayAccount != null && !autopayEnabled) {
            throw new BusinessException(ErrorCode.AUTOPAY_CONFLICT, "autopayEnabled=false while autopayAccountId is set.");
        }
    }
}
