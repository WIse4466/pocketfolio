package com.example.pocketfolio.controller;

import com.example.pocketfolio.dto.CreateCategoryRequest;
import com.example.pocketfolio.dto.CreateTransactionRequest;
import com.example.pocketfolio.entity.Account;
import com.example.pocketfolio.entity.AccountType;
import com.example.pocketfolio.entity.TransactionKind;
import com.example.pocketfolio.service.AccountService;
import com.example.pocketfolio.service.CategoryService;
import com.example.pocketfolio.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    private static final long MAX_SIZE = 2L * 1024 * 1024; // 2MB

    @PostMapping(value = "/ttjb", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importTtjb(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "createAccounts", defaultValue = "false") boolean createAccounts,
            @RequestParam(value = "createCategories", defaultValue = "false") boolean createCategories
    ) throws Exception {
        if (file.isEmpty()) return bad("Uploaded file is empty.");
        if (file.getSize() > MAX_SIZE) return bad("File too large. Max 2MB.");
        String ctype = Optional.ofNullable(file.getContentType()).orElse("");
        if (!(ctype.contains("csv") || ctype.equals("application/vnd.ms-excel") || ctype.equals("text/plain") || ctype.isBlank())) {
            return bad("Unsupported content-type: " + ctype);
        }
        UUID uid = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        List<Map<String, Object>> errors = new ArrayList<>();
        int imported = 0, skipped = 0, createdAcc = 0, createdCat = 0;

        // Load references
        List<Account> accounts = accountService.getAllAccounts();
        Map<String, Account> accountByName = new HashMap<>();
        for (Account a : accounts) accountByName.put(a.getName(), a);

        // Category name map from service (tree of DTO). We store only top-level for creation.
        Map<String, UUID> categoryByName = new HashMap<>();
        for (var root : categoryService.getCategories()) {
            categoryByName.put(root.getName(), root.getId());
            // also flatten children as "Parent/Child" keys for lookup
            Deque<Map.Entry<String, com.example.pocketfolio.dto.CategoryTreeDto>> stack = new ArrayDeque<>();
            stack.push(Map.entry(root.getName(), root));
            while (!stack.isEmpty()) {
                var e = stack.pop();
                var base = e.getKey();
                for (var ch : e.getValue().getChildren()) {
                    String key = base + "/" + ch.getName();
                    categoryByName.put(key, ch.getId());
                    stack.push(Map.entry(key, ch));
                }
            }
        }

        // Load all bytes once to allow multi-encoding attempts
        byte[] bytes = file.getBytes();
        List<CSVRecord> records = null;
        int headerRow = -1;
        Map<String, Integer> idx = null;

        List<java.nio.charset.Charset> encodings = List.of(StandardCharsets.UTF_8, java.nio.charset.Charset.forName("MS950"), java.nio.charset.Charset.forName("Big5"));
        char[] delims = new char[]{',', '\t', ';'};

        outer:
        for (java.nio.charset.Charset cs : encodings) {
            for (char delim : delims) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(bytes), cs))) {
                    CSVParser parser = CSVFormat.DEFAULT.builder().setSkipHeaderRecord(false).setDelimiter(delim).build().parse(br);
                    List<CSVRecord> recs = parser.getRecords();
                    if (recs.isEmpty()) continue;
                    // scan first 10 rows as potential header
                    int scan = Math.min(10, recs.size());
                    for (int h = 0; h < scan; h++) {
                        Map<String, Integer> candidate = indexHeaders(recs.get(h));
                        if (candidate.get("date") != null && (candidate.get("account") != null)) {
                            records = recs;
                            headerRow = h;
                            idx = candidate;
                            break outer;
                        }
                    }
                } catch (Exception ignore) { }
            }
        }

        if (records == null || headerRow < 0) {
            return bad("無法辨識 CSV 表頭，請確認包含『日期』與『帳戶』欄位，且為逗號/分號/Tab 分隔，編碼 UTF-8 或 Big5。");
        }

        for (int i = headerRow + 1; i < records.size(); i++) {
                CSVRecord row = records.get(i);
                try {
                    String dateStr = get(row, idx.get("date"));
                    String typeStr = get(row, idx.get("type"));
                    BigDecimal income = parseDecimal(get(row, idx.get("income")));
                    BigDecimal expense = parseDecimal(get(row, idx.get("expense")));
                    BigDecimal amountExplicit = parseDecimal(get(row, idx.get("amount")));
                    String accountName = get(row, idx.get("account"));
                    String categoryName = get(row, idx.get("category"));
                    String subcategoryName = get(row, idx.get("subcategory"));
                    String currencyCode = optUpper(get(row, idx.get("currency")));
                    String notes = Optional.ofNullable(get(row, idx.get("notes"))).orElse("");

                    if (isBlank(dateStr) || isBlank(accountName)) throw new IllegalArgumentException("Missing 日期/帳戶");

                    TransactionKind kind = resolveKind(typeStr, income, expense);
                    if (kind == null) throw new IllegalArgumentException("Cannot determine kind");
                    if (kind == TransactionKind.TRANSFER) throw new IllegalArgumentException("TRANSFER not supported in this importer");

                    BigDecimal amount = amountExplicit;
                    if (amount == null) amount = (kind == TransactionKind.INCOME) ? income : expense;
                    if (amount == null) throw new IllegalArgumentException("Invalid amount");
                    if (amount.compareTo(BigDecimal.ZERO) < 0) {
                        amount = amount.abs();
                        // flip kind if present, otherwise infer EXPENSE for negative
                        if (kind == null) kind = TransactionKind.EXPENSE;
                        else if (kind == TransactionKind.INCOME) kind = TransactionKind.EXPENSE;
                        else if (kind == TransactionKind.EXPENSE) kind = TransactionKind.INCOME;
                    }
                    if (amount.compareTo(BigDecimal.ZERO) == 0) throw new IllegalArgumentException("Invalid amount");

                    Account account = accountByName.get(accountName);
                    if (account == null && createAccounts) {
                        Account a = new Account();
                        a.setUserId(uid);
                        a.setName(accountName);
                        a.setType(AccountType.CASH);
                        a.setCurrencyCode(Optional.ofNullable(currencyCode).orElse("TWD"));
                        a.setInitialBalance(BigDecimal.ZERO);
                        a.setCurrentBalance(BigDecimal.ZERO);
                        a.setIncludeInNetWorth(true);
                        a.setArchived(false);
                        account = accountService.createAccount(a);
                        accountByName.put(accountName, account);
                        createdAcc++;
                    }
                    if (account == null) throw new IllegalArgumentException("Account not found: " + accountName);

                    String txCurrency = Optional.ofNullable(currencyCode).orElse(account.getCurrencyCode());
                    Instant occurredAt = toTpeMidnight(dateStr);

                    UUID categoryId = null;
                    // Normalize names to avoid creating a child with the same name as its parent
                    String parentName = isBlank(categoryName) ? null : categoryName.trim();
                    String childName = isBlank(subcategoryName) ? null : subcategoryName.trim();
                    if (parentName != null && childName != null && parentName.equals(childName)) {
                        childName = null; // treat as only parent category
                    }

                    if (parentName != null) {
                        // ensure parent exists
                        UUID parentId = categoryByName.get(parentName);
                        if (parentId == null && createCategories) {
                            var req = new CreateCategoryRequest();
                            req.setName(parentName);
                            var created = categoryService.createCategory(req);
                            parentId = created.getId();
                            categoryByName.put(parentName, parentId);
                            createdCat++;
                        }
                        if (childName != null) {
                            String key = parentName + "/" + childName;
                            UUID cid = categoryByName.get(key);
                            if (cid == null && createCategories && parentId != null) {
                                var req = new CreateCategoryRequest();
                                req.setName(childName);
                                req.setParentId(parentId);
                                var created = categoryService.createCategory(req);
                                cid = created.getId();
                                categoryByName.put(key, cid);
                                createdCat++;
                            }
                            categoryId = cid;
                        } else {
                            categoryId = parentId; // use parent as category if no child provided
                        }
                    } else if (childName != null) {
                        // only child name provided; try existing top-level matching this name, else create as top-level if allowed
                        UUID cid = categoryByName.get(childName);
                        if (cid == null && createCategories) {
                            var req = new CreateCategoryRequest();
                            req.setName(childName);
                            var created = categoryService.createCategory(req);
                            cid = created.getId();
                            categoryByName.put(childName, cid);
                            createdCat++;
                        }
                        categoryId = cid;
                    }

                    CreateTransactionRequest req = new CreateTransactionRequest();
                    req.setUserId(uid);
                    req.setKind(kind);
                    req.setAmount(amount);
                    req.setOccurredAt(occurredAt);
                    req.setAccountId(account.getId());
                    req.setCategoryId(categoryId);
                    req.setNotes(notes);
                    req.setCurrencyCode(txCurrency);

                    transactionService.createTransaction(req);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    Map<String, Object> err = new HashMap<>();
                    err.put("row", i + 1);
                    err.put("message", e.getMessage());
                    errors.add(err);
                }
            }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imported", imported);
        body.put("skipped", skipped);
        body.put("createdAccounts", createdAcc);
        body.put("createdCategories", createdCat);
        body.put("errors", errors);
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String, Object>> bad(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("error", "ValidationError");
        m.put("message", msg);
        return ResponseEntity.badRequest().body(m);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String get(CSVRecord row, Integer idx) {
        if (idx == null) return null;
        String v = row.size() > idx ? row.get(idx) : null;
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static Map<String, Integer> indexHeaders(CSVRecord header) {
        Map<String, Integer> map = new HashMap<>();
        List<String> cols = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            cols.add(header.get(i) == null ? "" : normalizeHeader(header.get(i)));
        }

        // Helper to find index by preferred exact names, then contains
        java.util.function.BiFunction<List<String>, List<String>, Integer> findIdx = (preferredExact, fallbacks) -> {
            for (int i = 0; i < cols.size(); i++) {
                String c = cols.get(i);
                for (String ex : preferredExact) if (c.equals(normalizeHeader(ex))) return i;
            }
            for (int i = 0; i < cols.size(); i++) {
                String c = cols.get(i);
                for (String fb : fallbacks) if (!fb.isEmpty() && c.contains(normalizeHeader(fb))) return i;
            }
            return null;
        };

        Integer idxDate = findIdx.apply(List.of("日期", "date", "Date"), List.of());
        if (idxDate != null) map.put("date", idxDate);

        Integer idxType = findIdx.apply(List.of("收支區分", "收支", "收支別", "類型", "类型", "type", "Type"), List.of());
        if (idxType != null) map.put("type", idxType);

        Integer idxAmount = findIdx.apply(List.of("金額", "金额", "金額(元)", "金額(新台幣)", "amount", "Amount"), List.of());
        if (idxAmount != null) map.put("amount", idxAmount);

        Integer idxIncome = findIdx.apply(List.of("收入", "income", "Income"), List.of());
        if (idxIncome != null) map.put("income", idxIncome);

        Integer idxExpense = findIdx.apply(List.of("支出", "expense", "Expense"), List.of());
        if (idxExpense != null) map.put("expense", idxExpense);

        Integer idxAccount = findIdx.apply(List.of("帳戶", "账户", "帳本", "資金帳戶", "account", "Account"), List.of());
        if (idxAccount != null) map.put("account", idxAccount);

        Integer idxCategory = findIdx.apply(List.of("大類別", "主分類", "分類", "分类", "category", "Category"), List.of("大類"));
        if (idxCategory != null) map.put("category", idxCategory);

        Integer idxSub = findIdx.apply(List.of("類別", "子分類", "子类別", "子分类", "subcategory", "Subcategory"), List.of());
        if (idxSub != null) map.put("subcategory", idxSub);

        Integer idxCcy = findIdx.apply(List.of("貨幣", "幣別", "币别", "币種", "幣種", "currency", "Currency"), List.of());
        if (idxCcy != null) map.put("currency", idxCcy);

        Integer idxNotes = findIdx.apply(List.of("備註", "备注", "notes", "Notes", "說明"), List.of());
        if (idxNotes != null) map.put("notes", idxNotes);

        // Avoid mapping category and subcategory to the same column; if equal, prefer keeping category and drop subcategory
        if (map.containsKey("category") && map.containsKey("subcategory") && Objects.equals(map.get("category"), map.get("subcategory"))) {
            map.remove("subcategory");
        }
        return map;
    }

    private static String normalizeHeader(String s) {
        String t = s == null ? "" : s.trim();
        // Strip BOM and zero-width characters that may prefix the first header cell
        t = t.replace("\uFEFF", ""); // BOM
        t = t.replace("\u200B", ""); // zero-width space
        // remove bracketed/parentheses content and spaces
        t = t.replaceAll("[\\(（].*?[\\)）]", "");
        t = t.replaceAll("[\\s\u3000]", "");
        return t.toLowerCase(Locale.ROOT);
    }

    private static TransactionKind resolveKind(String typeStr, BigDecimal income, BigDecimal expense) {
        if (!isBlank(typeStr)) {
            String t = typeStr.trim().toLowerCase(Locale.ROOT);
            if (t.contains("income") || t.contains("收入") || t.equals("收")) return TransactionKind.INCOME;
            if (t.contains("expense") || t.contains("支出") || t.equals("支")) return TransactionKind.EXPENSE;
            if (t.contains("transfer") || t.contains("轉帳") || t.contains("转账")) return TransactionKind.TRANSFER;
        }
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0) return TransactionKind.INCOME;
        if (expense != null && expense.compareTo(BigDecimal.ZERO) > 0) return TransactionKind.EXPENSE;
        return null;
    }

    private static BigDecimal parseDecimal(String s) {
        if (isBlank(s)) return null;
        String n = s.replace(",", "");
        try { return new BigDecimal(n); } catch (Exception e) { return null; }
    }

    private static String optUpper(String s) { return isBlank(s) ? null : s.trim().toUpperCase(Locale.ROOT); }

    private static Instant toTpeMidnight(String ymd) {
        String s = ymd.trim();
        int y, m, d;
        if (s.matches("^\\d{8}$")) { // YYYYMMDD
            y = Integer.parseInt(s.substring(0,4));
            m = Integer.parseInt(s.substring(4,6));
            d = Integer.parseInt(s.substring(6,8));
        } else {
            String norm = s.replace('/', '-');
            String[] p = norm.split("-");
            if (p.length < 3) throw new IllegalArgumentException("Invalid date: " + ymd);
            y = Integer.parseInt(p[0]);
            m = Integer.parseInt(p[1]);
            d = Integer.parseInt(p[2]);
        }
        java.time.LocalDate ld = java.time.LocalDate.of(y, m, d);
        return ld.atStartOfDay(java.time.ZoneId.of("Asia/Taipei")).toInstant();
    }
}
