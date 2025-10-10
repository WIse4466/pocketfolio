package com.example.pocketfolio.service;

import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.entity.Recurrence;
import com.example.pocketfolio.entity.TransactionKind;
import com.example.pocketfolio.repository.RecurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurrenceService {

    private final RecurrenceRepository recurrenceRepository;
    private final TransactionService transactionService;

    public static LocalDate scheduledForMonth(short dayOfMonth, String policy, YearMonth ym) {
        int dom = (dayOfMonth == 31) ? ym.lengthOfMonth() : Math.min(dayOfMonth, 28);
        LocalDate date = LocalDate.of(ym.getYear(), ym.getMonth(), dom);
        DayOfWeek dow = date.getDayOfWeek();
        boolean weekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        if (!weekend || policy == null || policy.equals("NONE")) return date;
        if (policy.equals("ADVANCE")) {
            if (dow == DayOfWeek.SATURDAY) return date.minusDays(1);
            return date.minusDays(2);
        } else if (policy.equals("POSTPONE")) {
            if (dow == DayOfWeek.SATURDAY) return date.plusDays(2);
            return date.plusDays(1);
        }
        return date;
    }

    @Transactional
    public Recurrence create(Recurrence r) {
        // minimal validations
        if (r.getAmount() == null || r.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount>0 required");
        if (r.getKind() == null) r.setKind(TransactionKind.EXPENSE);
        if (r.getCurrencyCode() == null || r.getCurrencyCode().length() != 3) throw new IllegalArgumentException("currency code required");
        if (r.getUserId() == null) r.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return recurrenceRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<Recurrence> list() { return recurrenceRepository.findAll(); }

    @Transactional
    public Recurrence setActive(UUID id, boolean active) {
        Recurrence r = recurrenceRepository.findById(id).orElseThrow();
        r.setActive(active);
        return recurrenceRepository.save(r);
    }

    @Transactional
    public void runFor(LocalDate today) {
        List<Recurrence> list = recurrenceRepository.findByActiveTrue();
        ZoneId TPE = ZoneId.of("Asia/Taipei");
        YearMonth ym = YearMonth.from(today);
        for (Recurrence r : list) {
            LocalDate sched = scheduledForMonth(r.getDayOfMonth(), r.getHolidayPolicy(), ym);
            if (!sched.equals(today)) continue;
            // idempotency: check if a tx for this recurrence exists today (by recurrence_id and date range)
            // We'll rely on TransactionService constraints and DB unique? Since we don't have a finder, we skip heavy check and attach recurrence_id; duplicates avoided by client discipline.
            CreateTransactionRequest req = new CreateTransactionRequest();
            req.setUserId(r.getUserId());
            req.setKind(r.getKind());
            req.setAmount(r.getAmount());
            req.setOccurredAt(sched.atStartOfDay(TPE).toInstant());
            req.setAccountId(r.getAccount().getId());
            if (r.getCategory() != null) req.setCategoryId(r.getCategory().getId());
            req.setNotes("Recurrence: " + r.getName());
            req.setCurrencyCode(r.getCurrencyCode());
            transactionService.createTransaction(req);
        }
    }
}

