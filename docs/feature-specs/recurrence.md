# 功能規格｜固定週期交易與假日策略（草稿）

來源：Notion Ticket — https://www.notion.so/26cb9b4303cc8063a7b1f608eeaeb866?source=copy_link
Last synced: 2025-09-14

## 背景 / 目標
- 支援以 RRULE 定義固定週期交易（薪資、訂閱、水電等）。
- 遇假日可依策略：照常、提前至最近工作日、延後至下一工作日。

## 使用者敘述與驗收（AC）
- Given 我設定每月 1 號入帳薪資（提前至工作日）；When 1 號為假日；Then 系統於最近前一個工作日建立收入交易。

## 規則與邊界
- RRULE：遵循 RFC 5545（BYDAY/BYMONTH/BYSETPOS 等）子集；搭配 `timezone` 與 `start_date`/`end_date`。
- 假日來源：台灣例假日＋週末；資料可快取於 DB（每年匯入）。
- 產生策略：可設定提前/延後的「最遠」天數上限，避免跨月錯位。
- 產生結果：以單筆交易寫入，記錄 `source_bill_id` 與 `run_id` 追蹤來源。

## 資料模型 / API（草案）
- Schedule：`rrule`, `timezone`, `start_date`, `end_date`。
- Bill：`name`, `amount`, `holiday_policy`, `next_run_at`, `active`。
- API：
  - POST `/bills/{id}/run`（手動觸發一次）
  - GET `/bills?next_run_before=...`（查待執行清單）

## 排程與冪等
- 每日 00:05 掃描 `next_run_at <= now()` 的 Bill；產生後更新下一次 `next_run_at`。
- 冪等 key：`bill_id + scheduled_window`；重跑不得重複建立交易。

## 例外 / 錯誤
- 規則無法解析：標記 Bill 為錯誤狀態並通知使用者。
- 產生交易失敗（DB 交易衝突）：重試並記錄。

## 追蹤
- 可觀測：產生交易數、成功率、重試次數、假日策略分佈。
