# Branching Strategy (Lightweight)

本專案目前採用簡化的 GitHub Flow，適合個人開發與 MVP 階段。

- main：預設穩定分支，可部署版本
- feature/*：功能開發（例：feature/cc-liability）
- fix/*：錯誤修正（例：fix/export-date-bug）
- chore/*、docs/*：日常維護、文件變更

工作流程
- 從 main 切出分支（feature/* 或 fix/*）
- 提交小步快跑、保持可通過測試
- 開 PR 目標 main，CI 綠燈後執行 squash merge
- 合併後刪除工作分支（遠端與本地）

測試與啟動（摘要）
- 後端單元測試：`cd backend && ./gradlew test --info`
- 全服務啟動：`docker-compose up -d --build`

備註
- 若遇到 Flyway 基準重整或破壞式遷移，開發環境可用 `docker-compose down -v` 重建資料庫（會清空本地 DB）。
