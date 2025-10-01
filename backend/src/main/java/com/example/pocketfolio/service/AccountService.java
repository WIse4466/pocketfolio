package com.example.pocketfolio.service;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.AccountType;
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
            account.setAutopayAccount(autopayAccount);
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
            existingAccount.setAutopayAccount(newAutopayAccount);
        } else {
            existingAccount.setAutopayAccount(null);
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
}
