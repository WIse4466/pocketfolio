package com.example.pocketfolio.controller;

import com.example.pocketfolio.service.FxService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
public class FxController {

    private final FxService fxService;

    @GetMapping("/rates")
    public ResponseEntity<List<Map<String, Object>>> rates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "TWD") String base
    ) {
        return ResponseEntity.ok(fxService.listRates(date, base.toUpperCase()));
    }

    @PutMapping("/rates")
    public ResponseEntity<Void> upsert(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "TWD") String base,
            @RequestParam String quote,
            @RequestParam BigDecimal rate
    ) {
        fxService.upsertRate(date, base.toUpperCase(), quote.toUpperCase(), rate);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/net-worth")
    public ResponseEntity<Map<String, Object>> netWorth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "TWD") String base
    ) {
        return ResponseEntity.ok(fxService.netWorthTwd(date, base.toUpperCase()));
    }
}

