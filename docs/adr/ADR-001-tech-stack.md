# ADR-001 : 前後端技術棧 2025.09.14-v1

## 背景 / 問題

- 專案「超級記帳程式」進入 MVP 階段，需要盡快定版技術棧以避免後續重工。
- 核心需求包含：記帳（收入/支出/轉帳）、信用卡帳單（結帳日/扣款日、自動扣款）、固定週期交易（含遇假日調整規則）、資產管理（先支援現金/帳戶，後續擴充證券/加密資產）。
- 期望：快速開發、良好型別安全與資料一致性、能可靠執行排程工作（產生週期交易、扣款、提醒）、未來可擴充（多幣別、報表、API 擴展）。
- 團隊學習目標：優先熟悉 Spring Boot 與主流企業級後端實務；前端以 React 為主流生態。

## 決策

我們決定採用 **React（Vite + TypeScript） + Spring Boot（Java 21） + PostgreSQL** 作為 MVP 技術棧。

輔助套件與約定：

- 後端：Spring Web / Spring Data JPA(Hibernate) / Validation、Flyway（DB 版本控管）、Actuator（健康檢查/指標）、Lombok（樣板碼減少，若不喜可後續移除）、OpenAPI/Swagger（API 文件）。
- 排程：Spring Scheduling（@Scheduled + cron，時區預設 Asia/Taipei），後續如需更進階可評估 Quartz。
- 前端：React + Vite + TypeScript，狀態管理（Zustand 或 Redux Toolkit，MVP 可先輕量），UI（可選 shadcn/ui 或 MUI）。
- DB：PostgreSQL（金額用 `NUMERIC(18,2)`；交易採 ACID 交易保證；必要欄位加上外鍵/唯一鍵/檢核約束；可用 JSONB 做可選延伸欄位）。
- 安全性：MVP 暫不實作帳號系統；先開 CORS + API Key（簡易）做節流，後續導入 Spring Security + JWT / Keycloak。
- 部署：本機/開發用 Docker Compose（frontend/backend/postgres/pgAdmin）；CI 用 GitHub Actions（build/test/Lint）。

## 替代方案

- **A：Next.js（全端）+ Prisma + PostgreSQL（全 TypeScript）**
    
    優點：單一語言、開發迭代快、SSR/ISR 方便。缺點：長期複雜商業邏輯與排程生態分散（需另建 worker/Cron），型別安全跨層仍需自律，ORM 成熟度/遷移策略與 JPA 相比仍有差異。
    
- **B：Django + DRF + PostgreSQL（Python）**
    
    優點：CRUD/管理後台極速、學習門檻低。缺點：可靠排程/任務通常需 Celery/Redis，部署與營運件數增；強型別不足，後期規模化可維運性較考驗工程規範。
    
- **C：Supabase（後端即服務）+ React**
    
    優點：上手最快、認證/RLS/即時性開箱即用、低維運。缺點：雲鎖定與复杂業務邏輯/排程可控性較弱；遇到複雜財務交易與批次對帳邏輯時容易受限。
    

## 取捨與影響

- **效能**：Spring Boot 性能穩定、對高併發與批次任務成熟；PostgreSQL 提供可靠事務一致性，適合金流/轉帳類情境。
- **複雜度**：Java/Spring 儀式感較重、初期樣板多；但結構清晰、分層與規範完整，長期維運成本低。
- **成本**：自管 Postgres + 單一後端服務器即可起步（Docker Compose）；雲成本可控。開發成本前期略高，但換取長期穩定與擴充性。
- **風險**：
    - 團隊熟悉度：需花時間建立 Spring/JPA 最佳實務（避免 N+1、交易邊界、鎖策略）。
    - 排程正確性：信用卡/週期性任務需考慮時區與假日調整、冪等性（重跑不重複扣款）。
    - 資料模型演進：MVP 先簡化（單幣別/不做複式簿記），後續導入更嚴謹的科目/分錄模型需做遷移與回填。

## 後續行動

- **文件**
    - 建立《系統架構草案》《API 規格（OpenAPI）》與《資料模型（ERD v0.1）》：Account / Transaction / Category；`Transaction.kind ∈ {income, expense, transfer}`。
    - 《週期任務規格》：信用卡（結帳/扣款日、預設扣款帳戶）、週期交易（假日三策略：照常/提前/延後）、冪等規則與審計日誌。
- **程式碼**
    - 建立 mono-repo 或前後端雙 repo；初始化：
        - backend：Spring Boot 3、Java 21、Gradle/Maven、模組分層（api/application/domain/infrastructure）、Flyway、Actuator、OpenAPI。
        - frontend：Vite + React + TS、路由/狀態管理、環境變數、API SDK（由 OpenAPI 生成）。
        - db：`V1__init.sql`（基礎表與索引/約束）、開發用 seed（預設分類/帳戶）。
    - 實作最小用例：新增交易、轉帳（同交易內原帳戶扣款＋目標帳戶入帳，單一交易包覆於 DB 交易中）。
    - 排程骨架：`@Scheduled` 任務（Asia/Taipei），實作冪等 key（例如以「規則ID+週期窗口」判定）。
- **監控**
    - 開啟 Actuator `/health`、`/metrics`；日誌統一 Logback 格式；預留 Prometheus/Grafana 介接。
    - 基礎異常告警（API 5xx、排程失敗重試次數超閾值）。
- **里程碑**
    - M1：基礎記帳/轉帳/分類（2 週）
    - M2：信用卡帳單 & 扣款排程（2 週）
    - M3：週期交易（含假日調整）（1–2 週）
    - M4：資產頁雛型與報表（1–2 週）

（決策棧：React + Spring Boot + PostgreSQL）

