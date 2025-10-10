package com.example.pocketfolio.service;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FxService {

    private final JdbcTemplate jdbcTemplate;
    private final AccountRepository accountRepository;

    public List<Map<String, Object>> listRates(LocalDate date, String base) {
        return jdbcTemplate.query("select quote_ccy, rate from fx_rates where as_of_date=? and base_ccy=? order by quote_ccy",
                (rs, rn) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("quote", rs.getString(1));
                    m.put("rate", rs.getBigDecimal(2));
                    return m;
                }, java.sql.Date.valueOf(date), base);
    }

    public void upsertRate(LocalDate date, String base, String quote, BigDecimal rate) {
        int updated = jdbcTemplate.update("update fx_rates set rate=?, updated_at=now() where as_of_date=? and base_ccy=? and quote_ccy=?",
                rate, java.sql.Date.valueOf(date), base, quote);
        if (updated == 0) {
            jdbcTemplate.update("insert into fx_rates(as_of_date, base_ccy, quote_ccy, rate) values(?,?,?,?)",
                    java.sql.Date.valueOf(date), base, quote, rate);
        }
    }

    public Map<String, Object> netWorthTwd(LocalDate date, String base) {
        Map<String, BigDecimal> rateByCcy = new HashMap<>();
        // base -> 1
        rateByCcy.put(base.toUpperCase(Locale.ROOT), BigDecimal.ONE);
        jdbcTemplate.query("select quote_ccy, rate from fx_rates where as_of_date=? and base_ccy=?",
                rs -> {
                    rateByCcy.put(rs.getString(1).toUpperCase(Locale.ROOT), rs.getBigDecimal(2));
                }, java.sql.Date.valueOf(date), base.toUpperCase(Locale.ROOT));

        BigDecimal total = BigDecimal.ZERO;
        List<Account> accounts = accountRepository.findAll();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Account a : accounts) {
            if (!a.isIncludeInNetWorth()) continue;
            String ccy = a.getCurrencyCode() != null ? a.getCurrencyCode().toUpperCase(Locale.ROOT) : base;
            BigDecimal rate = rateByCcy.getOrDefault(ccy, null);
            BigDecimal twd = null;
            if (rate != null) {
                twd = a.getCurrentBalance().multiply(rate);
                total = total.add(twd);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accountId", a.getId());
            m.put("name", a.getName());
            m.put("currency", ccy);
            m.put("balance", a.getCurrentBalance());
            m.put("twd", twd);
            items.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("base", base);
        out.put("date", date.toString());
        out.put("netWorthTwd", total);
        out.put("items", items);
        out.put("rates", rateByCcy);
        return out;
    }
}

