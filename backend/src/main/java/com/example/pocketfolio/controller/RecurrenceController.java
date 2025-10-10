package com.example.pocketfolio.controller;

import com.example.pocketfolio.entity.Recurrence;
import com.example.pocketfolio.service.RecurrenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurrences")
@RequiredArgsConstructor
public class RecurrenceController {

    private final RecurrenceService recurrenceService;

    private Map<String, Object> toDto(Recurrence r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("kind", r.getKind());
        m.put("amount", r.getAmount());
        m.put("currencyCode", r.getCurrencyCode());
        Map<String, Object> acc = new LinkedHashMap<>();
        if (r.getAccount() != null) {
            acc.put("id", r.getAccount().getId());
            acc.put("name", r.getAccount().getName());
            acc.put("currencyCode", r.getAccount().getCurrencyCode());
        }
        m.put("account", acc);
        if (r.getCategory() != null) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", r.getCategory().getId());
            m.put("category", cat);
        } else {
            m.put("category", null);
        }
        m.put("dayOfMonth", r.getDayOfMonth());
        m.put("holidayPolicy", r.getHolidayPolicy());
        m.put("active", r.isActive());
        return m;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Map<String, Object>> out = recurrenceService.list().stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Recurrence r) {
        Recurrence saved = recurrenceService.create(r);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<Map<String, Object>> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        Recurrence saved = recurrenceService.setActive(id, active);
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/run-today")
    public ResponseEntity<Void> runToday(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        recurrenceService.runFor(date);
        return ResponseEntity.noContent().build();
    }
}
