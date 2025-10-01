# Changelog

All notable changes to this project will be documented in this file.
This format follows Keep a Changelog and adheres to Semantic Versioning where practical.

## [Unreleased]

## 0.1.1 — 2025-10-01
### Added
- CI: 新增 `frontend-lint`（ESLint）與 `backend-test`（Gradle 測試）工作，與既有 build 分離並行。

### Changed
- DB/Flyway：重整為新基準（V1：users/categories/accounts；V2：預設使用者），移除舊有相互衝突的遷移。注意：本地需以 `docker compose down -v` 重置資料卷後重啟。
- CI：`backend-docker-build` 保留僅建置，不推送影像；整體工作拆分更清晰。

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
