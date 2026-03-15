package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.AccountBalanceResponse;
import com.pocketfolio.backend.dto.MonthlySummaryResponse;
import com.pocketfolio.backend.service.AccountService;
import com.pocketfolio.backend.service.StaticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "7. 統計分析", description = "財務統計與報表功能")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StaticsService staticsService;
    private final AccountService accountService;

    // 月度收支統計
    @GetMapping("monthly/{year}/{month}")
    @Operation(
            summary = "查詢月度收支統計",
            description = "取得指定月份的收入、支出總額及各類別佔比"
    )
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @PathVariable int year,
            @PathVariable int month
    ) {
        return ResponseEntity.ok(staticsService.getMonthlySummary(year, month));
    }

    // 所有帳戶餘額總覽
    @GetMapping("/account-balances")
    @Operation(
            summary = "查詢所有帳戶餘額總覽",
            description = "取得所有帳戶的當前餘額、變動金額及變動百分比"
    )
    public ResponseEntity<List<AccountBalanceResponse>> getAllAccountBalances() {
        return ResponseEntity.ok(accountService.getAllAccountBalances());
    }
}
