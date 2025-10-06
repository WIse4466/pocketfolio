package com.example.pocketfolio.service;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.AccountType;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.StatementRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BillingServiceTest {

    @Test
    void compute_due_date_handles_month_end_and_weekend() {
        // closing 2025-01-31, offset=1, dueDay=31 => 2025-02-(month end)
        LocalDate closing = LocalDate.of(2025,1,31);
        LocalDate due = BillingService.computeDueDate(closing, (short)1, 31, "NONE");
        assertEquals(LocalDate.of(2025,2,28), due);

        // weekend adjust: 2025-03-30 is Sunday; ADVANCE => 2025-03-28; POSTPONE => 2025-03-31
        LocalDate dt = LocalDate.of(2025,3,30);
        assertEquals(LocalDate.of(2025,3,28), BillingService.adjustHoliday(dt, "ADVANCE"));
        assertEquals(LocalDate.of(2025,3,31), BillingService.adjustHoliday(dt, "POSTPONE"));
    }
}

