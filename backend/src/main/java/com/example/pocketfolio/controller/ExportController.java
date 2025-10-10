package com.example.pocketfolio.controller;

import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.Transaction;
import com.example.pocketfolio.repository.AccountRepository;
import com.example.pocketfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping(value = "/csv", produces = "application/zip")
    public ResponseEntity<byte[]> exportCsvZip(@RequestParam(name = "v", defaultValue = "1") String version) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            writeAccountsCsv(zos);
            writeTransactionsCsv(zos);
            // ZipOutputStream will be closed by try-with-resources here
        }
        String filename = "export_" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(java.time.LocalDateTime.now()) + "_v" + version + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(baos.toByteArray());
    }

    private void writeAccountsCsv(ZipOutputStream zos) throws Exception {
        zos.putNextEntry(new ZipEntry("accounts.csv"));
        OutputStreamWriter w = new OutputStreamWriter(zos, StandardCharsets.UTF_8);
        w.write("id,name,type,currency_code,initial_balance,current_balance,archived,closing_day,due_day,autopay_account_id,created_at\n");
        List<Account> accounts = accountRepository.findAll();
        for (Account a : accounts) {
            String line = String.join(",",
                    q(a.getId()),
                    q(a.getName()),
                    q(a.getType() != null ? a.getType().name() : null),
                    q(a.getCurrencyCode()),
                    q(dec(a.getInitialBalance())),
                    q(dec(a.getCurrentBalance())),
                    q(Boolean.toString(a.isArchived())),
                    q(a.getClosingDay()),
                    q(a.getDueDay()),
                    q(a.getAutopayAccount() != null ? a.getAutopayAccount().getId() : null),
                    q(instant(a.getCreatedAt()))
            );
            w.write(line);
            w.write("\n");
        }
        w.flush(); // do not close the writer to keep ZipOutputStream open
        zos.closeEntry();
    }

    private void writeTransactionsCsv(ZipOutputStream zos) throws Exception {
        zos.putNextEntry(new ZipEntry("transactions.csv"));
        OutputStreamWriter w2 = new OutputStreamWriter(zos, StandardCharsets.UTF_8);
        w2.write("id,occurred_at,kind,amount,currency_code,account_id,source_account_id,target_account_id,category_id,notes,status,statement_id,created_at\n");
        List<Transaction> list = transactionRepository.findAll();
        for (Transaction t : list) {
            String line = String.join(",",
                    q(t.getId()),
                    q(instant(t.getOccurredAt())),
                    q(t.getKind() != null ? t.getKind().name() : null),
                    q(dec(t.getAmount())),
                    q(t.getCurrencyCode()),
                    q(t.getAccount() != null ? t.getAccount().getId() : null),
                    q(t.getSourceAccount() != null ? t.getSourceAccount().getId() : null),
                    q(t.getTargetAccount() != null ? t.getTargetAccount().getId() : null),
                    q(t.getCategory() != null ? t.getCategory().getId() : null),
                    q(t.getNotes()),
                    q(t.getStatus() != null ? t.getStatus().name() : null),
                    q(t.getStatement() != null ? t.getStatement().getId() : null),
                    q(instant(t.getCreatedAt()))
            );
            w2.write(line);
            w2.write("\n");
        }
        w2.flush(); // keep zip open
        zos.closeEntry();
    }

    private String q(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        // escape quotes and commas with simple CSV quoting
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private String dec(java.math.BigDecimal v) { return v == null ? null : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private String instant(java.time.Instant i) { return i == null ? null : DateTimeFormatter.ISO_INSTANT.format(i); }
}
