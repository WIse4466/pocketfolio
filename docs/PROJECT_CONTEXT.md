# PocketFolio 專案上下文

## 專案簡介

個人財務管理系統，支援多用戶、即時價格追蹤、智能警報與歷史分析。

**技術棧：**
- 後端：Spring Boot 3.5.10 + PostgreSQL 15 + Redis 7
- 前端：React 18 + TypeScript + Vite + Ant Design
- 認證：JWT Token
- 即時通訊：WebSocket + STOMP

---

## 🎯 專案目標

打造一個功能完整的個人財務管理系統，幫助用戶：
1. 追蹤日常收支
2. 管理多個帳戶
3. 投資資產追蹤（股票/加密貨幣）
4. 即時價格警報
5. 資料視覺化分析

---

## 📁 專案結構
```
pocketfolio/
├── backend/                 # Spring Boot 後端
│   ├── src/main/java/com/pocketfolio/backend/
│   │   ├── config/         # 配置類
│   │   ├── controller/     # REST 控制器
│   │   ├── dto/            # 數據傳輸對象
│   │   ├── entity/         # JPA 實體
│   │   ├── repository/     # JPA Repository
│   │   ├── service/        # 業務邏輯
│   │   ├── security/       # 安全相關
│   │   ├── scheduler/      # 定時任務
│   │   └── exception/      # 異常處理
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── static/
│   └── pom.xml
├── frontend/                # React 前端
│   ├── src/
│   │   ├── api/            # API 請求封裝
│   │   ├── components/     # 通用組件
│   │   ├── pages/          # 頁面組件
│   │   ├── store/          # Zustand 狀態管理
│   │   ├── types/          # TypeScript 類型
│   │   ├── utils/          # 工具函數
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── router.tsx
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── docs/                    # 專案文檔
│   ├── PROJECT_CONTEXT.md
│   ├── ARCHITECTURE.md
│   ├── CODING_STANDARDS.md
│   ├── API_REFERENCE.md
│   └── DEVELOPMENT_LOG.md
├── docker-compose.yaml
└── README.md
```

---

## 🔑 重要設計決策

### 1. 命名規範

**後端 User Entity：**
- `displayName` 而非 `username`（因為 UserDetails 介面已有 username）
- `email` 作為 UserDetails 的 username

### 2. 資料隔離

所有實體都有 `User` 關聯，Repository 自動過濾 `userId`：
```java
List<Transaction> findByUserId(UUID userId);
```

### 3. 循環依賴解決

**已解決：** PriceService ⇄ PriceAlertService
- PriceAlertService 不依賴 PriceService
- Controller 層負責組裝當前價格資料

### 4. API 設計原則

- RESTful 風格
- 統一錯誤處理（GlobalExceptionHandler）
- DTO Pattern（Request/Response 分離）
- JWT Token 認證（Bearer）

### 5. 前端狀態管理

- 認證狀態：Zustand（持久化到 localStorage）
- API 請求：Axios + 攔截器
- 表單狀態：Ant Design Form

---

## 🔐 環境變數

### 後端 (application.yaml)
```yaml
jwt:
  secret: [需要更改]
  expiration: 86400000

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pocketfolio
    username: admin
    password: password
  
  data:
    redis:
      host: localhost
      port: 6379

api:
  coingecko.base-url: https://api.coingecko.com/api/v3
  yahoo-finance.base-url: https://query1.finance.yahoo.com/v8/finance/chart
```

### 前端 (.env)
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

---

## 🚀 啟動指南

### 1. 啟動資料庫
```bash
docker-compose up -d
```

### 2. 啟動後端
```bash
cd backend
./mvnw spring-boot:run
```

### 3. 啟動前端
```bash
cd frontend
npm install
npm run dev
```

### 4. 訪問

- 前端：http://localhost:5173
- 後端 API：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html

---

## 📊 當前進度

### ✅ 已完成 (Phase 1-6)

**Phase 1-2: 基礎架構**
- [x] Spring Boot 專案設定
- [x] PostgreSQL + Docker
- [x] Transaction CRUD
- [x] Category, Account, Asset 實體

**Phase 3: 安全與多用戶**
- [x] JWT 認證
- [x] User Entity（實作 UserDetails）
- [x] 資料權限隔離
- [x] Spring Security 配置

**Phase 4: 即時整合**
- [x] Redis 快取配置
- [x] 外部 API（CoinGecko + Yahoo Finance）
- [x] PriceService 統一報價
- [x] @Scheduled 定時任務
- [x] WebSocket 配置
- [x] 價格警報功能
- [x] AssetSnapshot 歷史記錄
- [x] Swagger/OpenAPI 文檔

**Phase 5: React 前端基礎**
- [x] Vite + React + TypeScript
- [x] Ant Design UI
- [x] React Router 路由
- [x] Zustand 狀態管理
- [x] Axios 封裝
- [x] 登入/註冊頁面
- [x] 交易記錄 CRUD
- [x] 類別管理 CRUD
- [x] 帳戶管理 CRUD

**Phase 6: 進階功能 ✅ 已完成**
- [x] 資產管理頁面
- [x] 統計分析頁面（圖表）
- [x] 價格警報頁面
- [x] Dashboard 整合真實資料
- [x] WebSocket 即時價格更新（STOMP + SockJS 全域連線）
- [x] 資產歷史快照頁面（投資組合走勢圖）
- [x] 資料庫複合索引優化（user_id, date）

### ⬜ 待完成 (Phase 7)

**部署優先：**
- [ ] 後端 Dockerfile（multi-stage build）
- [ ] 前端 build 設定（環境變數指向 Cloud Run URL）
- [ ] GCP 環境設定（Cloud Run + Cloud SQL + Artifact Registry）
- [ ] GitHub Actions CI/CD Pipeline
- [ ] Upstash Redis 串接（取代本地 Redis）

**部署後持續改進：**
- [ ] Token 過期主動偵測（前端 UX 修復）
- [ ] 響應式優化（手機版）
- [ ] 效能優化（代碼分割、懶加載）
- [ ] 完整測試（單元測試 + 整合測試）

---

## 🎯 下一步工作

### 優先級 1（本週）

1. **WebSocket 即時更新**
   - 前端 WebSocket 客戶端連接
   - 價格更新即時推播
   - 警報通知推播

2. **資產歷史快照頁面**
   - 資產走勢圖表
   - 投資組合歷史

### 優先級 2（下週）

3. **響應式優化**
   - 手機版布局調整
   - 平板適配

4. **效能優化**
   - 前端代碼分割
   - 懶加載優化
   - API 請求優化

---

## ⚠️ 已知問題

### 後端

1. **Transaction.date 缺少索引**
   - 影響：查詢效能
   - 優先級：中
   - 計劃：Phase 7 處理

2. **Service 層用戶驗證重複代碼**
   - 影響：代碼維護性
   - 優先級：低
   - 計劃：考慮使用 AOP 或 BaseService

### 前端

1. **資產列表未實作完整載入邏輯**
   - 目前只支援單一投資帳戶
   - 需要實作多帳戶資產聚合

2. **圖表響應式待優化**
   - 手機版圖表顯示需要調整

---

## 📚 相關文檔

- [架構設計](./ARCHITECTURE.md)
- [編碼規範](./CODING_STANDARDS.md)
- [API 參考](./API_REFERENCE.md)
- [開發日誌](./DEVELOPMENT_LOG.md)

---

## 🤝 協作規範

### Git Commit 訊息格式
```
<type>(<scope>): <subject>

feat: 新功能
fix: 修復 bug
docs: 文檔更新
style: 代碼格式（不影響代碼運行）
refactor: 重構
test: 測試
chore: 建構過程或輔助工具的變動
```

### 分支策略

- `main`: 生產環境
- `develop`: 開發主分支
- `feature/*`: 功能分支
- `bugfix/*`: 修復分支

---

## 💡 提示

當使用 Claude Code 時：
1. 參考 `docs/` 目錄了解專案上下文
2. 查看 `TODO.md` 了解待辦事項
3. 遵循 `CODING_STANDARDS.md` 編碼規範
4. 更新 `DEVELOPMENT_LOG.md` 記錄變更