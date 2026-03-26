# 💰 PocketFolio - 個人財務管理系統

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18.2-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

> 一個功能完整的個人財務管理系統，支援多用戶、即時價格追蹤、智能警報與歷史分析。

![PocketFolio Banner](docs/images/banner.png)

---

## 🌟 核心功能

### ✅ 已完成功能 (Phase 1-6)

| 功能模組 | 說明 | 狀態 |
|---------|------|------|
| **🔐 用戶認證** | JWT Token 認證、註冊/登入、密碼加密 | ✅ 完成 |
| **💳 交易管理** | 收支記錄 CRUD、多維度篩選、分類統計 | ✅ 完成 |
| **📁 類別管理** | 收入/支出類別、自定義分類 | ✅ 完成 |
| **🏦 帳戶管理** | 多帳戶支援、餘額追蹤、四種帳戶類型 | ✅ 完成 |
| **💎 資產追蹤** | 股票/加密貨幣持倉、成本計算、損益分析 | ✅ 完成 |
| **📊 統計分析** | 月度收支圖表、分類佔比、趨勢分析 | ✅ 完成 |
| **💹 即時報價** | CoinGecko + Yahoo Finance API 整合 | ✅ 完成 |
| **🔔 價格警報** | 自動監控、條件觸發、即時通知 | ✅ 完成 |
| **📸 歷史快照** | 資產快照、趨勢分析、投資組合歷史 | ✅ 完成 |
| **⚡ 即時推播** | WebSocket 價格更新、警報通知 | ✅ 完成 |
| **📖 API 文檔** | Swagger/OpenAPI 3 互動式文檔 | ✅ 完成 |

### 🚧 開發中功能

- [ ] WebSocket 前端整合（即時價格顯示）
- [ ] 資產歷史走勢圖表
- [ ] 手機版響應式優化

### 📋 規劃中功能 (Phase 7)

- [ ] Docker 生產環境部署
- [ ] CI/CD Pipeline
- [ ] 完整測試覆蓋
- [ ] 效能優化
- [ ] 多語言支援

---

## 🛠️ 技術棧

### 後端技術

| 技術 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.10 | 主框架 |
| Spring Security | 6.x | 安全認證 |
| Spring Data JPA | 3.x | ORM 層 |
| PostgreSQL | 15 | 關聯式資料庫 |
| Redis | 7 | 快取層 |
| WebSocket + STOMP | - | 即時通訊 |
| JWT (jjwt) | 0.12.5 | Token 認證 |
| Swagger/OpenAPI | 3.0 | API 文檔 |

### 前端技術

| 技術 | 版本 | 用途 |
|------|------|------|
| React | 18.2 | UI 框架 |
| TypeScript | 5.2 | 型別系統 |
| Vite | 5.1 | 建構工具 |
| Ant Design | 5.15 | UI 組件庫 |
| Zustand | 4.5 | 狀態管理 |
| Axios | 1.6 | HTTP 客戶端 |
| Recharts | 2.x | 圖表庫 |
| React Router | 6.22 | 路由管理 |

### 外部服務

- **CoinGecko API** - 加密貨幣即時價格
- **Yahoo Finance API** - 股票即時報價

---

## 📦 專案結構
```
pocketfolio/
├── backend/                 # Spring Boot 後端
│   ├── src/main/java/com/pocketfolio/backend/
│   │   ├── config/         # 配置類（Security, Redis, WebSocket）
│   │   ├── controller/     # REST 控制器（37+ APIs）
│   │   ├── dto/            # 數據傳輸對象
│   │   │   ├── request/    # 請求 DTO
│   │   │   ├── response/   # 回應 DTO
│   │   │   ├── websocket/  # WebSocket 訊息
│   │   │   └── external/   # 外部 API DTO
│   │   ├── entity/         # JPA 實體（7 個主要實體）
│   │   ├── repository/     # JPA Repository
│   │   ├── service/        # 業務邏輯
│   │   │   └── external/   # 外部 API 服務
│   │   ├── security/       # JWT + Spring Security
│   │   ├── scheduler/      # 定時任務
│   │   └── exception/      # 異常處理
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── static/
│   │       └── ws-test.html  # WebSocket 測試頁
│   └── pom.xml
├── frontend/                # React 前端
│   ├── src/
│   │   ├── api/            # API 請求封裝（7 個服務）
│   │   ├── components/     # 通用組件
│   │   │   └── Layout/     # 主布局（Header + Sidebar）
│   │   ├── pages/          # 頁面組件（10+ 頁面）
│   │   │   ├── auth/       # 登入/註冊
│   │   │   ├── transactions/
│   │   │   ├── categories/
│   │   │   ├── accounts/
│   │   │   ├── assets/
│   │   │   ├── alerts/
│   │   │   └── statistics/
│   │   ├── store/          # Zustand 狀態管理
│   │   ├── types/          # TypeScript 類型定義
│   │   ├── utils/          # 工具函數
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── router.tsx      # 路由配置
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── docs/                    # 專案文檔 📚
│   ├── PROJECT_CONTEXT.md
│   ├── CODING_STANDARDS.md
│   ├── TODO.md
│   ├── CLAUDE_CODE_GUIDE.md
│   └── API_REFERENCE.md
├── docker-compose.yaml      # Docker 服務配置
├── .clinerules             # Claude Code 配置
└── README.md               # 本文件
```

---

## 🚀 快速開始

### 前置需求

確保已安裝以下工具：

- **Java 17+** ([下載](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.8+** (或使用專案內的 mvnw)
- **Node.js 18+** ([下載](https://nodejs.org/))
- **Docker & Docker Compose** ([下載](https://www.docker.com/))

### 1️⃣ 克隆專案
```bash
git clone https://github.com/yourusername/pocketfolio.git
cd pocketfolio
```

### 2️⃣ 啟動資料庫服務
```bash
# 啟動 PostgreSQL + Redis
docker-compose up -d

# 檢查服務狀態
docker ps
```

**預期輸出：**
```
CONTAINER ID   IMAGE            STATUS         PORTS
abc123...      postgres:15      Up 10 seconds  0.0.0.0:5432->5432/tcp
def456...      redis:7-alpine   Up 10 seconds  0.0.0.0:6379->6379/tcp
```

### 3️⃣ 配置後端

編輯 `backend/src/main/resources/application.yaml`：
```yaml
jwt:
  secret: your-super-secret-key-change-this-in-production-at-least-256-bits-long
  expiration: 86400000  # 24 小時

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pocketfolio
    username: admin
    password: password
  
  data:
    redis:
      host: localhost
      port: 6379
```

### 4️⃣ 啟動後端
```bash
cd backend

# 編譯並啟動
./mvnw spring-boot:run

# 或使用 IDE (IntelliJ IDEA / VS Code)
```

**成功啟動後會看到：**
```
Started BackendApplication in 12.345 seconds
Swagger UI: http://localhost:8080/swagger-ui.html
```

### 5️⃣ 配置前端

編輯 `frontend/.env`：
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

### 6️⃣ 啟動前端
```bash
cd frontend

# 安裝依賴
npm install

# 啟動開發伺服器
npm run dev
```

**成功啟動後會看到：**
```
VITE v5.1.0  ready in 500 ms

➜  Local:   http://localhost:5173/
```

### 7️⃣ 訪問應用

| 服務 | URL | 說明 |
|------|-----|------|
| 前端應用 | http://localhost:5173 | React 應用 |
| 後端 API | http://localhost:8080 | Spring Boot |
| Swagger UI | http://localhost:8080/swagger-ui.html | API 文檔 |
| WebSocket 測試 | http://localhost:8080/ws-test.html | WS 測試頁 |

---

## 📚 使用指南

### 首次使用

1. **註冊帳號**
   - 訪問 http://localhost:5173
   - 點擊「立即註冊」
   - 填寫 Email、顯示名稱、密碼

2. **建立基礎資料**
   - 登入後前往「類別管理」建立收入/支出類別
   - 前往「帳戶管理」建立銀行/現金帳戶
   - （可選）建立投資帳戶用於追蹤資產

3. **開始記帳**
   - 前往「交易記錄」新增收支記錄
   - 查看「統計分析」了解財務狀況

4. **投資追蹤**（進階）
   - 前往「資產管理」新增股票/加密貨幣
   - 設定「價格警報」監控價格變動
   - 查看資產走勢圖表

---

## 🔐 認證流程

### 註冊新用戶
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "displayName": "使用者名稱",
  "password": "password123"
}
```

**回應：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "使用者名稱"
}
```

### 登入
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### 使用 Token

所有業務 API 都需要在 Header 帶入 Token：
```http
GET /api/transactions
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 📊 API 概覽

### 認證 API（公開）

| 方法 | 端點 | 說明 |
|------|------|------|
| POST | `/api/auth/register` | 註冊新用戶 |
| POST | `/api/auth/login` | 用戶登入 |

### 業務 API（需要 Token）

| 模組 | 端點 | 數量 | 說明 |
|------|------|------|------|
| 交易記錄 | `/api/transactions` | 5 | CRUD + 篩選 |
| 類別管理 | `/api/categories` | 5 | CRUD + 類型篩選 |
| 帳戶管理 | `/api/accounts` | 5 | CRUD + 類型/搜尋 |
| 資產管理 | `/api/assets` | 5 | CRUD + 帳戶關聯 |
| 價格管理 | `/api/prices` | 5 | 查詢/更新/快取 |
| 價格警報 | `/api/price-alerts` | 6 | CRUD + 啟用切換 |
| 統計分析 | `/api/statistics` | 2 | 月度統計/帳戶餘額 |
| 資產快照 | `/api/snapshots` | 4 | 快照/歷史趨勢 |

**總計：37 個 REST API**

完整 API 文檔請訪問 [Swagger UI](http://localhost:8080/swagger-ui.html)

---

## ⚡ 核心特性

### 🔒 安全性

- **JWT Token 認證** - 24 小時有效期
- **密碼加密** - BCrypt 演算法
- **資料隔離** - 用戶只能存取自己的資料
- **CORS 保護** - 限制來源網域
- **SQL 注入防護** - 參數化查詢

### 📈 效能優化

- **Redis 快取** - 價格資料 5 分鐘 TTL
- **@Cacheable 註解** - Spring Cache 抽象
- **資料庫索引** - 關鍵欄位索引
- **分頁查詢** - 避免大量資料載入
- **懶加載** - JPA Lazy Fetch

### 🔄 即時功能

- **WebSocket 連接** - SockJS + STOMP
- **價格更新推播** - `/topic/price-updates`
- **警報通知** - `/user/queue/alerts`
- **系統訊息** - `/topic/system`

### 📊 資料視覺化

- **月度收支圖表** - 圓餅圖 + 柱狀圖
- **分類佔比分析** - 收入/支出明細
- **資產走勢圖** - 歷史價格趨勢
- **投資組合追蹤** - 損益分析

---

## ⚙️ 定時任務

| 任務 | 執行時間 | 說明 |
|------|---------|------|
| 價格更新 | 每 5 分鐘 | 更新所有資產的即時價格 |
| 快照建立 | 每天凌晨 1 點 | 建立每日資產快照 |
| 快取清除 | 每天凌晨 3 點 | 清除過期的價格快取 |

可在 `application.yaml` 中配置：
```yaml
scheduler:
  price-update:
    enabled: true
    cron: "0 */5 * * * *"
```

---

## 🧪 測試

### 後端測試
```bash
cd backend

# 執行所有測試
./mvnw test

# 執行特定測試
./mvnw test -Dtest=TransactionServiceTest

# 生成測試報告
./mvnw test jacoco:report
```

### 前端測試
```bash
cd frontend

# 建構測試
npm run build

# 預覽生產版本
npm run preview
```

### API 測試

**方式 1：Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

**方式 2：REST Client**
- 使用 `api-test.http`（VS Code REST Client 插件）
- 或 IntelliJ IDEA HTTP Client

---

## 🔧 故障排除

### ❌ 無法連接資料庫

**檢查：**
```bash
docker ps | grep postgres
docker logs pocketfolio-postgres
```

**解決：**
```bash
docker-compose down
docker-compose up -d
```

### ❌ Redis 連線失敗

**檢查：**
```bash
docker exec -it pocketfolio-redis redis-cli ping
# 應回傳 PONG
```

### ❌ JWT Token 過期

**解決：** 重新登入取得新 Token

### ❌ WebSocket 連線失敗

**檢查：**
- CORS 設定（`SecurityConfig.java`）
- 防火牆是否阻擋 8080 port
- 瀏覽器 Console 是否有錯誤

### ❌ 前端 API 請求 401

**檢查：**
```javascript
// 開啟瀏覽器 Console
localStorage.getItem('token')
```

**解決：** 清除 localStorage 並重新登入

---

## 📝 開發指南

### 新增一個 API

1. **後端步驟：**
```
   entity/ → repository/ → dto/ → service/ → controller/
```

2. **前端步驟：**
```
   types/ → api/ → pages/
```

3. **更新路由** (`router.tsx`)

4. **更新側邊欄** (`MainLayout.tsx`)

詳細指南請參考 [CLAUDE_CODE_GUIDE.md](docs/CLAUDE_CODE_GUIDE.md)

### 編碼規範

請遵循 [CODING_STANDARDS.md](docs/CODING_STANDARDS.md)

---

## 📋 待辦事項

查看 [TODO.md](docs/TODO.md) 了解：
- 🔴 高優先級任務
- 🟡 中優先級任務
- 🟢 低優先級任務

---

## 🐳 Docker 部署

### 開發環境
```bash
# 僅啟動資料庫服務
docker-compose up -d
```

### 生產環境（Phase 7 實作）
```bash
# 建構並啟動所有服務
docker-compose -f docker-compose.prod.yaml up -d
```

---

## 🤝 貢獻指南

### Git Commit 訊息格式
```
<type>(<scope>): <subject>

範例：
feat(auth): 新增 JWT Token 刷新功能
fix(transaction): 修復日期篩選錯誤
docs(readme): 更新安裝說明
refactor(service): 提取共用驗證邏輯
test(transaction): 新增交易建立測試
chore(deps): 升級 Spring Boot 到 3.5.11
```

### 分支策略

- `main` - 生產環境
- `develop` - 開發主分支
- `feature/*` - 功能分支
- `bugfix/*` - 修復分支

---

## 📖 文檔

| 文檔 | 說明 |
|------|------|
| [PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | 專案上下文與設計決策 |
| [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | 編碼規範與最佳實踐 |
| [TODO.md](docs/TODO.md) | 待辦事項與開發計劃 |
| [CLAUDE_CODE_GUIDE.md](docs/CLAUDE_CODE_GUIDE.md) | Claude Code 使用指南 |
| [API_REFERENCE.md](docs/API_REFERENCE.md) | API 詳細參考 |

---

## 📊 專案統計

### 代碼統計

- **後端代碼：** ~15,000 行
- **前端代碼：** ~8,000 行
- **配置文件：** ~1,000 行
- **測試代碼：** ~2,000 行

### 功能統計

- **實體數量：** 7 個
- **API 端點：** 37 個
- **前端頁面：** 10+ 個
- **組件數量：** 20+ 個

---

## 🎯 開發進度

### Phase 1-2: 基礎架構 ✅
- Spring Boot + PostgreSQL
- CRUD 功能
- 基礎實體設計

### Phase 3: 安全與多用戶 ✅
- JWT 認證
- 資料隔離
- Spring Security

### Phase 4: 即時整合 ✅
- Redis 快取
- 外部 API
- WebSocket
- 定時任務

### Phase 5: React 前端 ✅
- 基礎頁面
- 狀態管理
- API 整合

### Phase 6: 進階功能 🚧
- 資產管理 ✅
- 統計圖表 ✅
- 價格警報 ✅
- WebSocket 前端整合 ⏳
- 響應式優化 ⏳

### Phase 7: 部署與測試 📅
- Docker 生產環境
- CI/CD Pipeline
- 完整測試
- 效能優化

---

## ⚠️ 已知問題

### 待修復

1. **Transaction.date 缺少索引** - 影響查詢效能
2. **Service 層用戶驗證重複代碼** - 可使用 AOP 優化
3. **資產列表未實作多帳戶聚合** - 目前僅支援單一投資帳戶

完整清單請參考 [TODO.md](docs/TODO.md)

---

## 📄 授權

MIT License

---

## 🙏 致謝

感謝以下開源專案：

- [Spring Boot](https://spring.io/projects/spring-boot) - 後端框架
- [React](https://react.dev/) - 前端框架
- [Ant Design](https://ant.design/) - UI 組件庫
- [CoinGecko](https://www.coingecko.com/en/api) - 加密貨幣價格 API
- [Recharts](https://recharts.org/) - 圖表庫

---

## 📧 聯絡方式

- **Email:** 91211wise@gmail.com
- **GitHub:** https://github.com/Wise4466/pocketfolio
- **Issues:** https://github.com/Wise4466/pocketfolio/issues

---

## 🎉 開始使用
```bash
# 快速啟動（假設已安裝 Docker）
docker-compose up -d
cd backend && ./mvnw spring-boot:run &
cd frontend && npm install && npm run dev
```

訪問 http://localhost:5173 開始使用！

---

**⭐ 如果這個專案對你有幫助，請給個 Star！**