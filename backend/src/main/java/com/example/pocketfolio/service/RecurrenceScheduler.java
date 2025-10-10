package com.example.pocketfolio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class RecurrenceScheduler {

    private final RecurrenceService recurrenceService;
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    // Daily 00:05 TPE
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Taipei")
    public void daily() {
        LocalDate today = LocalDate.now(TAIPEI);
        recurrenceService.runFor(today);
    }
}

