# PocketFolio

個人財務管理系統，支援多帳戶、資產追蹤、即時報價與歷史分析。

**線上體驗：[https://pocketfolio-prod.web.app](https://pocketfolio-prod.web.app)**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Cloud Run](https://img.shields.io/badge/GCP-Cloud%20Run-4285F4.svg)](https://cloud.google.com/run)
[![Firebase](https://img.shields.io/badge/Firebase-Hosting-FFCA28.svg)](https://firebase.google.com/)

---

## 快速開始

**前置需求：** Java 17、Node.js 20、Docker

```bash
# 1. 啟動本地資料庫
docker-compose up -d

# 2. 後端（http://localhost:8080）
cd backend && ./mvnw spring-boot:run

# 3. 前端（http://localhost:5173）
cd frontend && npm install && npm run dev
```

其他端點：
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- WebSocket 測試：`http://localhost:8080/ws-test.html`

---

## 主要功能

- 收支交易記錄、分類管理、多帳戶支援
- 轉帳（Double-Entry Lite）— TRANSFER_OUT / TRANSFER_IN 配對
- 資產持倉追蹤，購買時可自動從指定帳戶扣款
- 即時報價（CoinGecko + Yahoo Finance）、WebSocket 推播
- 價格警報、每日資產快照、統計圖表
- JWT 認證、資料隔離、Redis 快取

---

## 文檔

| 文檔 | 說明 |
|------|------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 系統架構、技術棧、API 概覽、部署流程 |
| [TODO.md](docs/TODO.md) | 待辦事項與開發進度 |
| [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | 編碼規範 |
| [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | 專案背景與設計決策 |
| [dev_logs/](docs/dev_logs/) | 開發日誌 |

---

## CI/CD

推送到 `main` 後自動觸發 GitHub Actions：

- `backend/**` 變更 → Docker build → 部署 Cloud Run
- `frontend/**` 變更 → `npm run build` → 部署 Firebase Hosting
- 所有 PR 和非 main 分支 push → 執行後端測試 + 前端 lint/build

---

## 聯絡

- Email: 91211wise@gmail.com
- GitHub: https://github.com/Wise4466/pocketfolio
