package com.example.pocketfolio.service;

import com.example.pocketfolio.entity.*;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.StatementRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BillingServiceIdempotencyTest {

    @Test
    void autopay_skips_when_posted_payment_exists() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionRepository txRepo = mock(TransactionRepository.class);
        StatementRepository stmtRepo = mock(StatementRepository.class);

        BillingService svc = new BillingService(accountRepository, txRepo, stmtRepo);

        // Prepare a credit card and a CLOSED statement due today
        Account card = new Account();
        card.setId(UUID.randomUUID());
        card.setType(AccountType.CREDIT_CARD);
        card.setCurrencyCode("TWD");
        card.setAutopayEnabled(true);
        Account bank = new Account();
        bank.setId(UUID.randomUUID());
        bank.setType(AccountType.BANK);
        bank.setCurrencyCode("TWD");
        bank.setCurrentBalance(new BigDecimal("1000.00"));
        card.setAutopayAccount(bank);

        Statement s = new Statement();
        s.setId(UUID.randomUUID());
        s.setAccount(card);
        s.setDueDate(LocalDate.of(2025, 1, 1));
        s.setStatus(StatementStatus.CLOSED);
        s.setBalance(new BigDecimal("500.00"));

        when(stmtRepo.findByDueDateAndStatus(eq(LocalDate.of(2025,1,1)), eq(StatementStatus.CLOSED)))
                .thenReturn(List.of(s));
        // Simulate already posted once
        when(txRepo.countByStatement_IdAndStatus(eq(s.getId()), eq(TransactionStatus.POSTED))).thenReturn(1L);

        svc.autopayDueStatements(LocalDate.of(2025,1,1));

        // Allow planned transaction amount sync, but ensure NO POSTED payment is created
        verify(txRepo, never()).save(argThat(t -> t != null && TransactionStatus.POSTED.equals(t.getStatus())));
    }
}
