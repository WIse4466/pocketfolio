package com.example.pocketfolio.controller;

import com.example.pocketfolio.dto.AccountDto;
import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private AccountDto toDto(Account a) {
        return AccountDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .name(a.getName())
                .type(a.getType())
                .currencyCode(a.getCurrencyCode())
                .initialBalance(a.getInitialBalance())
                .currentBalance(a.getCurrentBalance())
                .includeInNetWorth(a.isIncludeInNetWorth())
                .archived(a.isArchived())
                .closingDay(a.getClosingDay())
                .dueDay(a.getDueDay())
                .autopayAccountId(a.getAutopayAccount() != null ? a.getAutopayAccount().getId() : null)
                .dueMonthOffset(a.getDueMonthOffset())
                .dueHolidayPolicy(a.getDueHolidayPolicy())
                .autopayEnabled(a.isAutopayEnabled())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody Account account) {
        Account createdAccount = accountService.createAccount(account);
        return new ResponseEntity<>(toDto(createdAccount), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        List<AccountDto> dtos = accounts.stream().map(this::toDto).collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable UUID id) {
        Account account = accountService.getAccountById(id);
        return new ResponseEntity<>(toDto(account), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable UUID id, @RequestBody Account account) {
        Account updatedAccount = accountService.updateAccount(id, account);
        return new ResponseEntity<>(toDto(updatedAccount), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        accountService.deleteAccount(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
