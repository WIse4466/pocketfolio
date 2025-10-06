# 功能規格｜信用卡結帳與自動扣款（草稿）

來源：Notion Ticket — https://www.notion.so/26cb9b4303cc8063a7b1f608eeaeb866?source=copy_link
Last synced: 2025-09-14

## 背景 / 目標
- 將信用卡消費視為負債；於扣款日自動產生「轉帳：扣款帳戶 → 信用卡」交易。
- 支援結帳日、扣款日設定；遇假日可搭配假日策略（與週期引擎共用）。

## 使用者敘述與驗收（AC）
- Given 信用卡設定好結帳/扣款日與預設扣款帳戶；When 到扣款日（依假日策略調整）；Then 系統建立轉帳交易，沖銷本期負債。

## 規則與邊界（MVP 實作節錄）
- 結帳邏輯：計算結帳區間內「未入前期帳單」的信用卡消費合計，生成當期帳單（可查閱/匯出）。
- 扣款邏輯：扣款日建立轉帳交易（扣款帳戶 → 信用卡）。不足額時允許部分沖銷，餘額遞延。
- 狀態：`statement.status ∈ {open, closed, paid, partial}`；交易需與 `statement_id` 關聯以利對帳。
- 時區：預設 `Asia/Taipei`；跨時區以帳戶持有人時區為準。

## 資料模型 / API（草案）
- Account：新增 `closing_day`, `due_day`, `autopay_account_id`（可為空）。
- Statement：`period_start/end`, `closing_date`, `due_date`, `statement_balance`, `status`。
- API：
  - POST `/credit-cards/{id}/statements/close`（手動結帳）
  - POST `/credit-cards/{id}/statements/{sid}/autopay`（重試自動扣款）
  
  MVP 已提供等效端點：
  - `POST /api/billing/credit-cards/{accountId}/close?date=YYYY-MM-DD`
  - `POST /api/billing/autopay?date=YYYY-MM-DD`

## 排程與冪等
- 每日 00:20 執行扣款掃描（cron，Asia/Taipei）。
- 冪等 key：`statement_id + run_window`；重跑不得重複生成轉帳。

## 例外 / 錯誤
- 扣款帳戶餘額不足：記錄部分扣款、通知使用者。
- 設定缺漏：跳過並告警；於健康檢查頁面提示。

## 追蹤
- 可觀測：生成帳單數、成功扣款率、重試次數、失敗原因分佈。
