package com.example.pocketfolio.controller;

import com.example.pocketfolio.entity.Recurrence;
import com.example.pocketfolio.service.RecurrenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurrences")
@RequiredArgsConstructor
public class RecurrenceController {

    private final RecurrenceService recurrenceService;

    @GetMapping
    public ResponseEntity<List<Recurrence>> list() {
        return ResponseEntity.ok(recurrenceService.list());
    }

    @PostMapping
    public ResponseEntity<Recurrence> create(@RequestBody Recurrence r) {
        return ResponseEntity.ok(recurrenceService.create(r));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<Recurrence> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(recurrenceService.setActive(id, active));
    }

    @PostMapping("/run-today")
    public ResponseEntity<Void> runToday(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        recurrenceService.runFor(date);
        return ResponseEntity.noContent().build();
    }
}

