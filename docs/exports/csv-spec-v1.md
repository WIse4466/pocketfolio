# CSV 匯出規格 v1

目的：提供一致、可被 Excel/BI 工具匯入的 CSV 匯出格式，支援多幣別並以基準幣別 TWD 顯示換算欄位。

版本：v1（2025-09-14）

通用規範
- 編碼：UTF-8（為提升 Excel 相容性，可選擇加 BOM）
- 分隔符：逗號 `,`；換行：LF（`\n`）
- 首列為標題列；必要欄位不可為空
- 日期：`YYYY-MM-DD`；日期時間：ISO 8601，例 `YYYY-MM-DDTHH:mm:ss+08:00`
- 金額：十進位字串（兩位小數），例 `12345.67`
- 欄位命名：全小寫、底線分隔；ID 為 UUID（如存在）
- 多幣別：`amount` 與 `currency_code` 搭配；`fx_rate` 定義為「1 單位原幣 = x TWD」；`base_amount_twd = amount * fx_rate`

檔案清單
- transactions.csv
- accounts.csv
- categories.csv
- statements.csv
- bills.csv
- bill_payments.csv
- exchange_rates.csv

transactions.csv
- 欄位：
  - `id`、`account_id`、`occurred_at`、`posted_at`、`kind`（`income|expense|transfer`）
  - `amount`、`currency_code`、`fx_rate`、`base_amount_twd`
  - `category_id`、`note`、`status`（`confirmed|void|pending`）
  - `transfer_group_id`、`counterparty_account_id`、`statement_id`、`bill_id`
  - `created_at`

accounts.csv
- 欄位：`id`、`name`、`type`（`cash|bank|credit_card|e_wallet|brokerage|crypto_wallet`）、`currency_code`、`initial_balance`、`balance`、`archived`、`closing_day`、`due_day`、`autopay_account_id`、`created_at`

categories.csv
- 欄位：`id`、`name`、`type`（`income|expense|transfer`）、`parent_id`、`builtin`

statements.csv（信用卡帳單）
- 欄位：`id`、`account_id`、`period_start`、`period_end`、`closing_date`、`due_date`、`statement_balance`、`min_payment`、`payment_transaction_id`、`status`（`open|closed|paid|partial`）

bills.csv（固定週期模板）
- 欄位：`id`、`name`、`amount`、`currency_code`、`holiday_policy`（`as_is|advance|delay`）、`active`、`schedule_rrule`、`timezone`、`start_date`、`end_date`、`next_run_at`、`default_account_id`、`default_category_id`、`template_note`

bill_payments.csv
- 欄位：`id`、`bill_id`、`paid_on`、`amount`、`account_id`、`note`、`is_partial`

exchange_rates.csv（當日或歷史匯率）
- 欄位：`as_of_date`、`base_ccy`、`quote_ccy`、`rate`

檔名與封裝
- 檔名採 `export_YYYYMMDD_HHMM_v1.zip`（ZIP 內含上述 CSV）

備註
- 單一使用者 MVP：`user_id` 欄位省略；未來支援多使用者時再加入
- 為避免浮點誤差，內部運算仍建議以整數最小單位儲存；CSV 對外輸出統一二位小數
