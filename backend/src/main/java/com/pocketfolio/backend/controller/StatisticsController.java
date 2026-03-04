package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AccountBalanceResponse;
import com.pocketfolio.backend.dto.MonthlySummaryResponse;
import com.pocketfolio.backend.service.AccountService;
import com.pocketfolio.backend.service.StaticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StaticsService staticsService;
    private final AccountService accountService;

    // 月度收支統計
    @GetMapping("monthly/{year}/{month}")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @PathVariable int year,
            @PathVariable int month
    ) {
        return ResponseEntity.ok(staticsService.getMonthlySummary(year, month));
    }

    // 所有帳戶餘額總覽
    @GetMapping("/account-balances")
    public ResponseEntity<List<AccountBalanceResponse>> getAllAccountBalances() {
        return ResponseEntity.ok(accountService.getAllAccountBalances());
    }
}
