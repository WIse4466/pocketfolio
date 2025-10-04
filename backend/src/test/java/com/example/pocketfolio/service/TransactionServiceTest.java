package com.example.pocketfolio.service;

import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.entity.*;
import com.example.pocketfolio.exception.BusinessException;
import com.example.pocketfolio.exception.NotFoundException;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.CategoryRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;
    private TransactionService service;

    private final Map<UUID, Account> accounts = new HashMap<>();
    private final Map<UUID, Transaction> store = new HashMap<>();

    @BeforeEach
    void setup() {
        transactionRepository = mock(TransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        service = new TransactionService(transactionRepository, accountRepository, categoryRepository);

        // repo behavior
        when(accountRepository.findById(ArgumentMatchers.any(UUID.class))).thenAnswer((Answer<Optional<Account>>) invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(accounts.get(id));
        });
        when(accountRepository.existsById(ArgumentMatchers.any(UUID.class))).thenAnswer((Answer<Boolean>) invocation -> {
            UUID id = invocation.getArgument(0);
            return accounts.containsKey(id);
        });

        when(transactionRepository.save(ArgumentMatchers.any(Transaction.class))).thenAnswer((Answer<Transaction>) invocation -> {
            Transaction tx = invocation.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            store.put(tx.getId(), tx);
            return tx;
        });
        when(transactionRepository.findById(ArgumentMatchers.any(UUID.class))).thenAnswer((Answer<Optional<Transaction>>) invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(store.get(id));
        });
        doAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            store.remove(id);
            return null;
        }).when(transactionRepository).deleteById(ArgumentMatchers.any(UUID.class));
        doAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            store.remove(tx.getId());
            return null;
        }).when(transactionRepository).delete(ArgumentMatchers.any(Transaction.class));
    }

    private Account mkAccount(AccountType type, String ccy, BigDecimal balance) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        a.setName(type + "-" + ccy);
        a.setType(type);
        a.setCurrencyCode(ccy);
        a.setInitialBalance(balance);
        a.setCurrentBalance(balance);
        a.setIncludeInNetWorth(true);
        a.setArchived(false);
        accounts.put(a.getId(), a);
        return a;
    }

    private CreateTransactionRequest baseReq(TransactionKind kind, BigDecimal amount) {
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        r.setKind(kind);
        r.setAmount(amount);
        r.setOccurredAt(Instant.parse("2025-01-01T00:00:00Z"));
        r.setCurrencyCode("TWD");
        return r;
    }

    @Test
    void expense_on_credit_card_makes_balance_more_negative_and_delete_rolls_back() {
        Account card = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("0.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.EXPENSE, new BigDecimal("100.00"));
        req.setAccountId(card.getId());

        Transaction tx = service.createTransaction(req);
        assertEquals(new BigDecimal("-100.00"), card.getCurrentBalance());

        service.deleteTransaction(tx.getId());
        assertEquals(new BigDecimal("0.00"), card.getCurrentBalance());
    }

    @Test
    void transfer_bank_to_credit_card_is_payment_and_delete_rolls_back() {
        Account bank = mkAccount(AccountType.BANK, "TWD", new BigDecimal("1000.00"));
        Account card = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("-100.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.TRANSFER, new BigDecimal("100.00"));
        req.setSourceAccountId(bank.getId());
        req.setTargetAccountId(card.getId());

        Transaction tx = service.createTransaction(req);
        assertEquals(new BigDecimal("900.00"), bank.getCurrentBalance());
        assertEquals(new BigDecimal("0.00"), card.getCurrentBalance());

        service.deleteTransaction(tx.getId());
        assertEquals(new BigDecimal("1000.00"), bank.getCurrentBalance());
        assertEquals(new BigDecimal("-100.00"), card.getCurrentBalance());
    }

    @Test
    void transfer_from_credit_card_is_forbidden() {
        Account card = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("-50.00"));
        Account bank = mkAccount(AccountType.BANK, "TWD", new BigDecimal("500.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.TRANSFER, new BigDecimal("10.00"));
        req.setSourceAccountId(card.getId());
        req.setTargetAccountId(bank.getId());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createTransaction(req));
        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void transfer_between_credit_cards_is_forbidden() {
        Account cardA = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("-50.00"));
        Account cardB = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("-30.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.TRANSFER, new BigDecimal("10.00"));
        req.setSourceAccountId(cardA.getId());
        req.setTargetAccountId(cardB.getId());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createTransaction(req));
        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void cross_currency_transfer_is_forbidden() {
        Account bankTwd = mkAccount(AccountType.BANK, "TWD", new BigDecimal("1000.00"));
        Account cardUsd = mkAccount(AccountType.CREDIT_CARD, "USD", new BigDecimal("-100.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.TRANSFER, new BigDecimal("50.00"));
        req.setSourceAccountId(bankTwd.getId());
        req.setTargetAccountId(cardUsd.getId());
        // tx currency must match source (service checks) -> set to TWD
        req.setCurrencyCode("TWD");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createTransaction(req));
        assertTrue(ex.getMessage().toLowerCase().contains("cross-currency"));
    }

    @Test
    void refund_on_credit_card_income_moves_towards_zero() {
        Account card = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("-200.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.INCOME, new BigDecimal("100.00"));
        req.setAccountId(card.getId());

        service.createTransaction(req);
        assertEquals(new BigDecimal("-100.00"), card.getCurrentBalance());
    }

    @Test
    void overpayment_results_in_positive_balance_and_is_allowed() {
        Account bank = mkAccount(AccountType.BANK, "TWD", new BigDecimal("1000.00"));
        Account card = mkAccount(AccountType.CREDIT_CARD, "TWD", new BigDecimal("-200.00"));

        CreateTransactionRequest req = baseReq(TransactionKind.TRANSFER, new BigDecimal("250.00"));
        req.setSourceAccountId(bank.getId());
        req.setTargetAccountId(card.getId());

        service.createTransaction(req);
        assertEquals(new BigDecimal("750.00"), bank.getCurrentBalance());
        assertEquals(new BigDecimal("50.00"), card.getCurrentBalance());
    }
}

