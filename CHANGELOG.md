# Changelog

All notable changes to this project will be documented in this file.
This format follows Keep a Changelog and adheres to Semantic Versioning where practical.

## [Unreleased]
### Added
- DB/Flyway V4：`accounts` 新增 `due_month_offset(0/1/2)`, `due_holiday_policy(NONE|ADVANCE|POSTPONE)`, `autopay_enabled`。
- 後端：`Account` 實體與 `AccountService` 驗證（DUE_MONTH_OFFSET_INVALID、DUE_HOLIDAY_POLICY_INVALID、AUTOPAY_NOT_SUPPORTED、AUTOPAY_ACCOUNT_INVALID、AUTOPAY_CONFLICT）。
- 前端：帳戶管理頁送出並讀取上述欄位（信用卡時）。
- Docs：`docs/api/accounts.md`、README 連結。
 - DB/Flyway V5：新增 `statements` 表；結帳與自動扣款 MVP（手動結帳端點、每日 00:20 自動扣款）。
 - 匯出：ZIP 內新增 `statements.csv`。
 - 預算：DB V8（`budgets`/`category_budgets`），API（summary/upsert），交易頁預算橫幅與設定表單。
 - 匯率：DB V9（`fx_rates`），API（`/api/fx/rates`、`/api/fx/net-worth`），前端「概覽」頁（設定匯率、TWD 淨值/等值）。
 - 固定週期（每月）：DB V10（`recurrences`），Scheduler（每日 00:05），API（建立/列表/啟停/今日執行），前端「排程」頁（表格/文字）。
 - 匯入：前端「匯入」頁（上傳 CSV），後端匯入器強化（欄位自動偵測、父/子分類建立、負數金額正規化、TPE 00:00）。

## 0.1.1 — 2025-10-01
### Added
- CI: 新增 `frontend-lint`（ESLint）與 `backend-test`（Gradle 測試）工作，與既有 build 分離並行。

### Changed
- DB/Flyway：重整為新基準（V1：users/categories/accounts；V2：預設使用者），移除舊有相互衝突的遷移。注意：本地需以 `docker compose down -v` 重置資料卷後重啟。
- CI：`backend-docker-build` 保留僅建置，不推送影像；整體工作拆分更清晰。
 - 匯出 ZIP：修正 Stream closed 問題，避免 Writer 提前關閉 ZipOutputStream。

### Fixed
- Frontend：修正 TypeScript `verbatimModuleSyntax` 與 `erasableSyntaxOnly` 導致的匯入/enum 問題；移除 `any` 用法以通過 ESLint。

### Chore / Docs
- Compose：移除過時的 `version: '3.8'` 欄位以消除警告。
- 文件：README 新增 CI 章節；DevOps 指南補充本倉庫 CI 現況與本機等效指令；本地開發指南新增 Flyway 基準重置說明。

## 0.1.0 — 2025-09-14
### Added
- README 更新為 MVP 規格
- ADR-001 技術棧、ADR-002 部署策略
- 技術草圖（C4/ERD）
- Roadmap（M1–M3）
- 功能規格骨架：信用卡結帳/自動扣款、固定週期交易
