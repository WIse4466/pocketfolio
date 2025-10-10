package com.example.pocketfolio.service;

import com.example.pocketfolio.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final JdbcTemplate jdbcTemplate;
    private final BudgetRepository budgetRepository;

    public Map<String, Object> summary(UUID userId, LocalDate month) {
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalLimit = jdbcTemplate.query("select total_limit from budgets where user_id=? and month=?", rs -> {
            if (rs.next()) return rs.getBigDecimal(1);
            return null;
        }, userId, java.sql.Date.valueOf(startDate));
        if (totalLimit == null) totalLimit = BigDecimal.ZERO;

        BigDecimal totalSpent = budgetRepository.totalExpense(userId, start, end);
        boolean overspend = totalLimit.compareTo(BigDecimal.ZERO) > 0 && totalSpent.compareTo(totalLimit) > 0;

        List<Map<String, Object>> categories = jdbcTemplate.query("select category_id, limit_amount from category_budgets where user_id=? and month=?", (rs, rowNum) -> {
            UUID cid = (UUID) rs.getObject(1);
            BigDecimal lim = rs.getBigDecimal(2);
            BigDecimal spent = budgetRepository.categoryExpense(userId, cid, start, end);
            boolean over = lim != null && lim.compareTo(BigDecimal.ZERO) > 0 && spent.compareTo(lim) > 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("categoryId", cid);
            m.put("limit", lim);
            m.put("spent", spent);
            m.put("overspend", over);
            return m;
        }, userId, java.sql.Date.valueOf(startDate));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", startDate.toString());
        out.put("totalLimit", totalLimit);
        out.put("totalSpent", totalSpent);
        out.put("overspend", overspend);
        out.put("categories", categories);
        return out;
    }

    public void upsertTotal(UUID userId, LocalDate month, BigDecimal limit) {
        int updated = jdbcTemplate.update("update budgets set total_limit=?, updated_at=now() where user_id=? and month=?", limit, userId, java.sql.Date.valueOf(month.withDayOfMonth(1)));
        if (updated == 0) {
            jdbcTemplate.update("insert into budgets(user_id, month, total_limit) values(?,?,?)", userId, java.sql.Date.valueOf(month.withDayOfMonth(1)), limit);
        }
    }

    public void upsertCategory(UUID userId, UUID categoryId, LocalDate month, BigDecimal limit) {
        int updated = jdbcTemplate.update("update category_budgets set limit_amount=?, updated_at=now() where user_id=? and category_id=? and month=?", limit, userId, categoryId, java.sql.Date.valueOf(month.withDayOfMonth(1)));
        if (updated == 0) {
            jdbcTemplate.update("insert into category_budgets(user_id, category_id, month, limit_amount) values(?,?,?,?)", userId, categoryId, java.sql.Date.valueOf(month.withDayOfMonth(1)), limit);
        }
    }
}

