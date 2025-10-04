# 超級記帳程式｜README

## 1. 問題 / 價值
現有工具不是偏「日常記帳」就是偏「資產總覽」。本專案要把兩者整合：  
**用最快的操作完成記帳，並即時看見淨值與資產分布。**

## 2. 目標（1 個可衡量）
我自己願意用，**連續 14 天每日使用**（記 1 筆以上）。

## 3. MVP 範圍（只列 Must）
### 3.1 日常記帳（Cash Flow）
- 交易 CRUD：金額、日期、分類/子分類、帳戶、備註
- 分類管理：系統預設＋自訂（含子分類）
- 帳戶管理：銀行、信用卡、現金、電子錢包、**證券/虛擬貨幣帳戶（計入資產）**
- **多幣別帳戶＋總資產匯率換算**（MVP：手動輸入當日匯率）
- **預算**：每月總額與分類預算；超支提醒（頁面內提醒即可）
- **固定週期交易（RRULE）＋假日策略**：照常 / 提前至最近工作日 / 延後至下一工作日

### 3.2 信用卡
- 信用卡消費列為**負債**（不影響當下現金帳戶）
- 還款以**轉帳**沖銷負債（扣款帳戶 → 信用卡）
- **結帳日 / 扣款日**設定；扣款日自動建立轉帳（模擬自動扣款）
  
  補充（MVP）：禁止信用卡作為轉出帳戶、禁止信用卡之間互轉；不支援跨幣轉帳。

### 3.3 資產概覽
- 淨資產 = 現金＋投資**手動市值**（預留行情 API 介面）
- **資產分布與歷史趨勢**（MVP：月/週級簡圖）

### 3.4 匯出
- **CSV/Excel 匯出**：交易明細、帳戶餘額、帳單/還款紀錄

> 先不做：自動分類/規則引擎、Forecast、分期/利息試算、對帳模式、延後入帳、免息期建議、Widget/捷徑/Watch（列為 Next）。

## 4. 成功指標
- **核心**：本人連續 14 天每日使用
- **健康**：崩潰率 < 1%，資料落差（帳戶總和 ≠ 淨值）= 0

## 5. 里程碑
- **M1：MVP**（交易/帳戶/分類、多幣別、信用卡結扣與自動扣款、預算、匯出）
- **M2：親友試用**（5 人）、回饋修正
- **M3：公開測試**（文件、示範資料、安裝指引）

## 6. 重大假設 / 風險（Top 3）
- 資料遺失風險 → **每日備份**、匯出 CSV、重要操作二次確認
- 使用者體驗 → 先「鍵盤優先」、「最近一次帶入」，再做進階 UI
- 匯率與市值手動成本 → 先支援「全域今日匯率」＋「批次市值更新」

## 7. 名詞定義
- **帳戶（Account）**：可持有餘額的容器（現金/銀行/信用卡/券商/加密錢包…）
- **交易（Transaction）**：`income | expense | transfer` 三類。轉帳不改變總資產。
- **負債（Liability）**：信用卡未清償金額視為負債，還款視為轉帳。
- **帳單（Bill）**：可重複（RRULE）產生之應付事件，支付後寫入交易。
- **匯率（FX）**：MVP 手動設定「當日全域匯率」。

## 8. 資料模型（草案）
- `accounts(id, name, type, currency, balance, created_at, updated_at)`
- `categories(id, name, parent_id)`
- `transactions(id, occurred_at, amount, kind, account_id, category_id, note, currency, fx_rate_used)`
- `bills(id, title, amount, currency, rrule, holiday_policy, next_due_on, account_id, category_id)`
- `bill_payments(id, bill_id, paid_on, amount, account_id, note, is_partial)`
- `fx_rates(id, as_of_date, base_ccy, quote_ccy, rate)` ＊可合併為設定表
> 已設計：`bills`、`bill_payments`。信用卡可用 `accounts.type = 'credit_card'`＋交易邏輯實作。

## 9. 驗收標準（AC）
- **新增支出**  
  Given 我在「新增交易」  
  When 金額 > 0、選定「帳戶/分類/日期」後儲存  
  Then 交易列表即時出現，所屬帳戶餘額同步扣減，總資產正確更新

- **信用卡扣款自動轉帳**  
  Given 信用卡的「結帳日/扣款日」已設定  
  When 進入扣款日（假日依策略調整）  
  Then 系統自動建立「轉帳：扣款帳戶 → 信用卡」之交易，信用卡負債歸零或遞延剩餘額

- **固定週期交易與假日策略**  
  Given 我設定每月 1 號入帳薪資（提前至工作日）  
  When 1 號為假日  
  Then 系統自動在最近前一個工作日建立該筆收入交易

## 10. NFR（極簡）
- 效能：P95 API < 500ms；首頁首屏 < 2.0s
- 正確性：交易導致的**帳戶餘額一致性**必須通過（含刪除/回滾）
- 安全：輸入驗證、密碼雜湊（若有登入）、.env 不入庫
- 可用性：每日自動備份；可一鍵匯出全量 CSV
- 在地化：時區 `Asia/Taipei`、金額兩位小數、貨幣顯示與符號正確

## 11. Next（非 MVP）
- 自動分類/規則引擎、Forecast（帶入排程/帳單）、信用卡分期/利息試算、對帳模式/延後入帳/免息期建議、Widget/捷徑/Watch、行情與匯率 API。

## 12. 如何啟動 (How to Run)

請確認您的電腦已安裝 Docker 與 Docker Compose。

1.  **啟動服務**
    在專案根目錄執行以下指令：
    ```bash
    docker-compose up -d
    ```
    此指令會啟動所有服務 (frontend, backend, db)。

2.  **存取服務**
    *   **前端應用程式**: [http://localhost:5173](http://localhost:5173)
    *   **後端 API 文件**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

3.  **停止服務**
    ```bash
    docker-compose down
    ```

> 啟動時遇到問題？請參考 [本地開發設定指南](docs/local-setup-guide.md)。

## 12.1 MVP：Transactions API（建立/查詢/刪除）

本階段提供「建立／查詢（區間）／刪除」交易的 API（MVP 僅單幣別）。完整說明見 docs/api/transactions.md。

- 建立：`POST /api/transactions`
- 查詢：`GET /api/transactions?from=&to=&page=&size=`（回傳 Page）
- 刪除：`DELETE /api/transactions/{id}`（含餘額回沖）

建立通用欄位：`userId`, `kind ∈ {INCOME,EXPENSE,TRANSFER}`, `amount>0`, `occurredAt(ISO8601)`, `notes?`, `currencyCode(3)`
類型特定欄位：
- INCOME/EXPENSE：`accountId`（必填），`categoryId?`
- TRANSFER：`sourceAccountId` + `targetAccountId`（皆必填、且不可相同）

範例請求：

收入（INCOME）
```json
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "kind": "INCOME",
  "amount": 1000.00,
  "occurredAt": "2025-01-01T00:00:00Z",
  "accountId": "<ACCOUNT_ID>",
  "categoryId": "<CATEGORY_ID>",
  "notes": "Salary",
  "currencyCode": "TWD"
}
```

支出（EXPENSE）
```json
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "kind": "EXPENSE",
  "amount": 250.50,
  "occurredAt": "2025-01-03T00:00:00Z",
  "accountId": "<ACCOUNT_ID>",
  "categoryId": "<CATEGORY_ID>",
  "notes": "Lunch",
  "currencyCode": "TWD"
}
```

轉帳（TRANSFER）
```json
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "kind": "TRANSFER",
  "amount": 500.00,
  "occurredAt": "2025-01-05T00:00:00Z",
  "sourceAccountId": "<SRC_ACCOUNT_ID>",
  "targetAccountId": "<DST_ACCOUNT_ID>",
  "notes": "Move funds",
  "currencyCode": "TWD"
}
```

標準錯誤格式（例）：
```json
{
  "error": "ValidationError",
  "message": "來源帳戶與目標帳戶不可相同"
}
```

限制（MVP）：
- 僅支援單一幣別；跨幣交易會被 400 拒絕。
- 信用卡特殊規則（結帳/繳款）尚未納入，先以一般加減計入帳戶餘額。

## 13. CI（持續整合）

本倉庫使用 GitHub Actions 進行最小可行驗證（前後端分開、可並行）：

- docs-link-check：Markdown 內部連結檢查（Lychee，離線）
- repo-sanity：列出檔案與 README 範圍預覽
- frontend-lint：Node 20，於 `frontend/` 執行 `npm ci` 與 `npm run lint`
- frontend-build：Node 20，於 `frontend/` 執行 `npm ci` 與 `npm run build`
- backend-test：以 `gradle:8.8.0-jdk21` 容器於 `backend/` 執行 `gradle test`
- backend-docker-build：使用 `backend/Dockerfile` 進行建置（驗證可編譯，不推鏡像）

本機等效指令（選擇性）：
- 前端 Lint/Build：`cd frontend && npm ci && npm run lint && npm run build`
- 後端測試（容器）：`docker run --rm -v "$PWD/backend":/home/gradle/project -w /home/gradle/project gradle:8.8.0-jdk21 gradle test`
- 後端 Docker 建置：`docker build -f backend/Dockerfile -t pocketfolio-backend-local:latest backend`

## 文件與連結
- ADR-001：前後端技術棧決策 — docs/adr/ADR-001-tech-stack.md
- ADR-002：部署策略 — docs/adr/ADR-002-deployment.md
- Transactions API — docs/api/transactions.md
- 技術草圖（C4-L1/L2/L3＋ERD）— docs/architecture/technical-sketch.md
- Roadmap — docs/roadmap.md
- Changelog — CHANGELOG.md
- DevOps 部署指南 — docs/devops/deployment-guide.md
- CSV 匯出規格 v1 — docs/exports/csv-spec-v1.md
