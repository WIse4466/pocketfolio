# ADR-002 : 部署策略 2025.09.14-v1

## 背景 / 問題

- 我們的 MVP 技術棧已定為 **React + Spring Boot + PostgreSQL**（ADR-001）。需要一套**可快速上線、易擴充、低維運成本**的部署策略，支援：
    - 前端靜態資源（SPA）與 API 反向代理/環境變數注入
    - 後端容器化、健康檢查、零停機或最小化停機部署
    - 定時任務（信用卡扣款日、週期交易）、時區 Asia/Taipei
    - 資料庫託管（備援/備份/遷移、連線池）
    - 基礎監控與告警、機密管理（API Key/DB 密碼）
- 團隊慣用 **GitHub** 做版控與 CI，期望透過 **GitHub Actions** 串接雲端/Paas 完成 CD。

## 決策

**MVP 決策（推薦）**

- **前端**：Vercel（或 Cloudflare Pages 二選一；先以 Vercel 為主）
- **後端**：Google Cloud Run（區域：asia-east1）
- **資料庫**：Neon（Serverless Postgres，自動休眠、便於分支預覽）
- **CI/CD**：GitHub Actions →（前端）Vercel Deploy、（後端）Build & Push 到 GHCR → Deploy 到 Cloud Run
- **排程**：Cloud Scheduler 觸發 Cloud Run Job / HTTPS endpoint（以冪等設計確保重試安全）
- **機密**：GitHub Environments Secrets + Vercel/Cloud Run 專案機密（後端敏感值優先放 Cloud Run Secret 參照）
- **監控**：Cloud Monitoring（後端），Vercel Analytics（前端）；後端開啟 Actuator /health /metrics

> 註：**GitHub 也能「部署網站」**指的是 GitHub Pages，偏向靜態網站託管（可放打包後的 React SPA），無法直接執行 Spring Boot 後端。因此此決策採「前端 Vercel、後端 Cloud Run」的混合式部署。

## 替代方案

**A｜全 PaaS 一站式：Render（前端/後端/DB/Cron 都在 Render）**

- 優點：介面整合、部署極簡、支援 Cron Job、Managed Postgres。
- 缺點：區域選擇有限、低流量冷啟延遲、免費/入門方案資源有限；流量成長後成本彈性不如 serverless「用多少付多少」。

**B｜全 GCP：Cloud Run + Cloud SQL + Cloud Scheduler + Cloud Load Balancing**

- 優點：單一雲供應商、可用 **asia-east1**（低延遲）、觀測性完整。
- 缺點：**Cloud SQL 不會 scale-to-zero**，MVP 固定成本較高；網路/代理與基建設定較繁瑣（VPC、連線池器）。

**C｜全 JS 生態：Vercel（前端）+ Vercel Serverless/Edge + 外部 Postgres（Neon/Supabase）**

- 優點：前端極速、預覽分支體驗佳、Database URL 一鍵注入。
- 缺點：我們後端是 Spring Boot，若要遷就 JS 伺服端需重寫；Serverless Java 支援度不如 Cloud Run。

**D｜Fly.io / Railway（容器＋簡易 DB/排程）**

- 優點：容易把 Docker 直接跑起來；靠近用戶的邊緣區域（如 NRT）。
- 缺點：平台成熟度/監控/網路設定細緻度略低；DB 選項與備援策略需要自行評估。

**E｜前端 GitHub Pages + 後端 Cloud Run（或 Render）+ 外部 Postgres**

- 優點：前端託管免費穩定；Pages 適合純靜態 SPA。
- 缺點：路由/404 回退需手動設定；缺少內建環境變數/Preview 機制，不如 Vercel 便利。

## 取捨與影響

- **效能**
    - Cloud Run：Cold start 對 Java 有感，但可透過最小執行個體與原生映像最佳化下降低；就地區而言 **asia-east1** 對台灣延遲最低。
    - Vercel：前端全球 CDN，加速靜態資源分發。
- **複雜度**
    - 混合式（Vercel + Cloud Run）清楚分工，但需管理兩套環境/Secrets。
    - 全 GCP 單雲一致性高，但雲資源學習曲線較陡。
- **成本**
    - MVP 月流量低時：**Cloud Run + Neon** 幾乎可趨近「用多少付多少」，固定成本低。
    - 成長期：Cloud SQL 穩定與效能更佳，但有固定月費；可屆時遷移（Flyway 管理 schema、離峰遷移計畫）。
- **風險**
    - **排程正確性**：必須以「冪等設計＋重試策略」避免重複扣款/生成交易。
    - **Vendor lock-in**：Vercel/Neon 生態舒適，但若全面轉單雲（GCP/AWS）需有遷移 Runbook。
    - **冷啟**：低流量時 Cloud Run 冷啟可能造成首請延遲；可設最小實例或以任務心跳預熱。

## 後續行動

**文件**

- 《部署手冊 v1》：分環境（dev/stage/prod）、網域與 DNS、SSL、Rollback 流程。
- 《排程與冪等規格》：任務鍵（rule_id+window）、重試次數、告警門檻。
- 《資料庫遷移計畫》：Neon → Cloud SQL 的 cutover 步驟與回滾策略。

**程式碼 / 基礎建設**

- GitHub Actions：
    - `frontend.yml`：安裝 → build → 部署到 Vercel（Preview on PR、Main → Prod）。
    - `backend.yml`：JDK 21 → Docker build → 推 GHCR → `gcloud run deploy`。
- 後端：Actuator `/health`、`/metrics`；結合 Cloud Monitoring 警示（5xx、冷啟延遲、p95）。
- 排程：Cloud Scheduler（Cron 表達式，時區 Asia/Taipei）→ 觸發 Cloud Run Job / HTTP endpoint。
- 機密：GitHub Environments + Vercel/Cloud Run Secrets；避免把機密寫入 `.env` 提交。
- CORS/反向代理：前端環境變數配置後端 API URL；必要時在 Cloud Run 前設置 Cloud Load Balancer 作路由。

**監控 / 運維**

- Error tracking（Sentry 選配）與日誌結構化（JSON）；
- 基礎 SLO：API 成功率 ≥ 99.5%、p95 延遲目標；
- 告警：部署失敗、排程失敗連續 N 次、DB 連線失敗、剩餘連線池不足。

---

### 附：我們常見的組合對照（速查）

| 組合 | 適用情境 | 優點 | 注意事項 |
| --- | --- | --- | --- |
| **Vercel（FE）+ Cloud Run（BE）+ Neon（DB）** | MVP、低流量、要省固定成本 | 前端體驗佳、後端 serverless、DB 休眠省錢 | 冷啟、雙平台管理 |
| **全 GCP（Cloud Run + Cloud SQL + Scheduler）** | 走長期、單雲治理 | 觀測性完整、低延遲、治理一致 | Cloud SQL 固定費用、初期較複雜 |
| **全 Render** | 快速一站式 | 簡單、Cron/DB 一體 | 區域/彈性/成本彈性 |
| **GitHub Pages（FE）+ Cloud Run（BE）** | 前端純靜態、成本極低 | 前端免費穩定 | FE 預覽體驗較弱、路由/404 需設定 |

> 總結：先走 Vercel + Cloud Run + Neon 拿速度與成本優勢；等功能/流量穩定再評估「全 GCP」以提升整體治理與可預期性能。

