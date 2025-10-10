package com.example.pocketfolio.controller;

import com.example.pocketfolio.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @RequestParam(required = false) UUID userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month
    ) {
        UUID uid = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        LocalDate m = LocalDate.parse(month + "-01");
        return ResponseEntity.ok(budgetService.summary(uid, m));
    }

    @PutMapping("/total")
    public ResponseEntity<Void> upsertTotal(
            @RequestParam(required = false) UUID userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month,
            @RequestParam BigDecimal limit
    ) {
        UUID uid = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        LocalDate m = LocalDate.parse(month + "-01");
        budgetService.upsertTotal(uid, m, limit);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/category")
    public ResponseEntity<Void> upsertCategory(
            @RequestParam(required = false) UUID userId,
            @RequestParam UUID categoryId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month,
            @RequestParam BigDecimal limit
    ) {
        UUID uid = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        LocalDate m = LocalDate.parse(month + "-01");
        budgetService.upsertCategory(uid, categoryId, m, limit);
        return ResponseEntity.noContent().build();
    }
}

