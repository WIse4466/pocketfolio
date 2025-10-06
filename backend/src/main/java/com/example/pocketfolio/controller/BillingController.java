package com.example.pocketfolio.controller;

import com.example.pocketfolio.entity.Statement;
import com.example.pocketfolio.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // Manual close for a given credit card account
    @PostMapping("/credit-cards/{accountId}/close")
    public ResponseEntity<Statement> close(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate d = (date != null) ? date : LocalDate.now();
        Statement s = billingService.closeForAccountOnDate(accountId, d);
        return ResponseEntity.ok(s);
    }

    // Manual autopay execution for statements due on date
    @PostMapping("/autopay")
    public ResponseEntity<Void> autopay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate d = (date != null) ? date : LocalDate.now();
        billingService.autopayDueStatements(d);
        return ResponseEntity.noContent().build();
    }
}

