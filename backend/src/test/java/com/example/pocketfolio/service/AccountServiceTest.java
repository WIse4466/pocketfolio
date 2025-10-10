package com.example.pocketfolio.service;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.AccountType;
import com.example.pocketfolio.exception.BusinessException;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import com.example.pocketfolio.repository.StatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AccountServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private StatementRepository statementRepository;
    private AccountService service;

    @BeforeEach
    void setup() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        statementRepository = mock(StatementRepository.class);
        service = new AccountService(accountRepository, transactionRepository, statementRepository);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    }

    private Account bank() {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        a.setName("Bank");
        a.setType(AccountType.BANK);
        a.setCurrencyCode("TWD");
        a.setInitialBalance(new BigDecimal("0"));
        a.setCurrentBalance(new BigDecimal("0"));
        a.setIncludeInNetWorth(true);
        a.setArchived(false);
        return a;
    }

    private Account card() {
        Account a = bank();
        a.setName("Card");
        a.setType(AccountType.CREDIT_CARD);
        return a;
    }

    @Test
    void bank_autopay_not_supported() {
        Account a = bank();
        a.setAutopayEnabled(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createAccount(a));
        assertTrue(ex.getMessage().toLowerCase().contains("autopay"));
    }

    @Test
    void invalid_offset_rejected() {
        Account a = card();
        a.setDueMonthOffset((short) 5);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createAccount(a));
        assertTrue(ex.getMessage().contains("0,1,2"));
    }

    @Test
    void invalid_holiday_policy_rejected() {
        Account a = card();
        a.setDueHolidayPolicy("FOO");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createAccount(a));
        assertTrue(ex.getMessage().contains("NONE"));
    }
}
