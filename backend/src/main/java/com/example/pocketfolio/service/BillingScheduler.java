package com.example.pocketfolio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private final BillingService billingService;

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    // Daily close at 00:10 TPE: for cards whose closingDay == today.day (31 means month end)
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Taipei")
    public void dailyClose() {
        LocalDate today = LocalDate.now(TAIPEI);
        billingService.autoCloseForDay(today);
    }

    // Daily autopay at 00:20 TPE for due statements
    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Taipei")
    public void dailyAutopay() {
        LocalDate today = LocalDate.now(TAIPEI);
        billingService.autopayDueStatements(today);
    }
}
