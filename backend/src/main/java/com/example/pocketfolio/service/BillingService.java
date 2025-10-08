package com.example.pocketfolio.service;

import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.entity.*;
import com.example.pocketfolio.exception.NotFoundException;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.StatementRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final TransactionService transactionService;

    @Transactional
    public Statement closeForAccountOnDate(UUID accountId, LocalDate closingDate) {
        Account card = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        if (card.getType() != AccountType.CREDIT_CARD) {
            throw new IllegalArgumentException("Only credit card accounts can be closed");
        }

        // Skip if already closed
        List<Statement> exists = statementRepository.findByAccountIdAndClosingDate(accountId, closingDate);
        if (!exists.isEmpty()) return exists.getFirst();

        // Determine period: previous closingDate(exclusive) -> this closingDate(inclusive)
        LocalDate prevClosing = previousClosingDate(closingDate, card.getClosingDay());
        LocalDate periodStart = prevClosing.plusDays(1);
        LocalDate periodEnd = closingDate;

        // Sum CC transactions (income refunds reduce balance, expense increases)
        Instant start = periodStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = periodEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal balance = transactionRepository.findAll().stream()
                .filter(tx -> tx.getAccount() != null && card.getId().equals(tx.getAccount().getId()))
                .filter(tx -> !tx.getOccurredAt().isBefore(start) && tx.getOccurredAt().isBefore(end))
                .map(tx -> switch (tx.getKind()) {
                    case EXPENSE -> tx.getAmount();
                    case INCOME -> tx.getAmount().negate();
                    default -> BigDecimal.ZERO; // TRANSFER ignored in statement balance
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate dueDate = computeDueDate(closingDate, card.getDueMonthOffset(), card.getDueDay(), card.getDueHolidayPolicy());

        Statement stmt = new Statement();
        stmt.setAccount(card);
        stmt.setPeriodStart(periodStart);
        stmt.setPeriodEnd(periodEnd);
        stmt.setClosingDate(closingDate);
        stmt.setDueDate(dueDate);
        stmt.setBalance(balance);
        stmt.setStatus(StatementStatus.CLOSED);
        return statementRepository.save(stmt);
    }

    @Transactional
    public void autoCloseForDay(LocalDate today) {
        // Find all credit cards and close those matching today as closing day (with 31 as month-end)
        for (Account card : accountRepository.findByType(AccountType.CREDIT_CARD)) {
            Integer cd = card.getClosingDay();
            if (cd == null) continue;
            boolean isMonthEnd = today.getDayOfMonth() == today.lengthOfMonth();
            if ((cd == 31 && isMonthEnd) || (cd != 31 && today.getDayOfMonth() == cd)) {
                closeForAccountOnDate(card.getId(), today);
            }
        }
    }

    @Transactional
    public void autopayDueStatements(LocalDate today) {
        List<Statement> due = statementRepository.findByDueDateAndStatus(today, StatementStatus.CLOSED);
        for (Statement s : due) {
            Account card = s.getAccount();
            if (!card.isAutopayEnabled() || card.getAutopayAccount() == null) continue;
            Account src = card.getAutopayAccount();
            BigDecimal amount = s.getBalance();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                s.setStatus(StatementStatus.PAID);
                statementRepository.save(s);
                continue;
            }
            BigDecimal available = src.getCurrentBalance();
            BigDecimal pay = available.min(amount);
            if (pay.compareTo(BigDecimal.ZERO) <= 0) continue;

            CreateTransactionRequest req = new CreateTransactionRequest();
            req.setUserId(card.getUserId());
            req.setKind(TransactionKind.TRANSFER);
            req.setAmount(pay);
            req.setOccurredAt(today.atStartOfDay(ZoneOffset.UTC).toInstant());
            req.setSourceAccountId(src.getId());
            req.setTargetAccountId(card.getId());
            req.setNotes("Autopay statement");
            req.setCurrencyCode(card.getCurrencyCode());
            transactionService.createTransaction(req);

            BigDecimal remaining = amount.subtract(pay);
            s.setBalance(remaining);
            s.setStatus(remaining.compareTo(BigDecimal.ZERO) == 0 ? StatementStatus.PAID : StatementStatus.PARTIAL);
            statementRepository.save(s);
        }
    }

    // Utilities
    public static LocalDate previousClosingDate(LocalDate closingDate, Integer closingDay) {
        if (closingDay == null || closingDay < 1 || closingDay > 31) {
            // fallback: previous month same date
            return closingDate.minusMonths(1);
        }
        LocalDate prev = closingDate.minusMonths(1);
        int dom = Math.min(closingDay, prev.lengthOfMonth());
        return LocalDate.of(prev.getYear(), prev.getMonth(), dom);
    }

    public static LocalDate computeDueDate(LocalDate closingDate, short monthOffset, Integer dueDay, String policy) {
        int offset = Math.max(0, Math.min(2, monthOffset));
        LocalDate base = closingDate.plusMonths(offset);
        int targetDay = (dueDay == null) ? base.getDayOfMonth() : Math.min(dueDay, 28);
        if (dueDay != null && dueDay == 31) {
            targetDay = base.lengthOfMonth();
        }
        LocalDate dt = LocalDate.of(base.getYear(), base.getMonth(), targetDay);
        return adjustHoliday(dt, policy);
    }

    public static LocalDate adjustHoliday(LocalDate date, String policy) {
        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        if (!isWeekend || policy == null || policy.equals("NONE")) return date;
        if (policy.equals("ADVANCE")) {
            // move to previous Friday
            if (dow == DayOfWeek.SATURDAY) return date.minusDays(1);
            return date.minusDays(2);
        } else if (policy.equals("POSTPONE")) {
            // move to next Monday
            if (dow == DayOfWeek.SATURDAY) return date.plusDays(2);
            return date.plusDays(1);
        }
        return date;
    }
}
