package com.example.pocketfolio.controller;

import com.example.pocketfolio.entity.Statement;
import com.example.pocketfolio.entity.StatementStatus;
import com.example.pocketfolio.service.BillingService;
import com.example.pocketfolio.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final StatementRepository statementRepository;

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

    // Query statements due within window (for calendar planned items)
    @GetMapping("/statements")
    public ResponseEntity<List<Statement>> listStatements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID accountId
    ) {
        List<Statement> list = (accountId != null)
                ? statementRepository.findByDueDateBetweenAndAccountId(from, to, accountId)
                : statementRepository.findByDueDateBetween(from, to);
        return ResponseEntity.ok(list);
    }
}
