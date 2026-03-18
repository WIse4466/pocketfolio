# PocketFolio - 個人財務管理系統

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

> 一個功能完整的個人財務管理系統，支援多用戶、即時價格追蹤、智能警報與歷史分析。

---

## 🌟 核心功能

### ✅ 已完成（Phase 1-4）

| 功能模組 | 說明 | 狀態 |
|---------|------|------|
| **用戶認證** | JWT Token、註冊/登入 | ✅ |
| **交易管理** | 收支記錄、分類統計 | ✅ |
| **帳戶管理** | 銀行/現金/信用卡/投資帳戶 | ✅ |
| **資產追蹤** | 股票/加密貨幣持倉管理 | ✅ |
| **即時報價** | CoinGecko + Yahoo Finance API | ✅ |
| **價格警報** | 自動監控價格變動並通知 | ✅ |
| **歷史記錄** | 資產快照與趨勢分析 | ✅ |
| **即時推播** | WebSocket 價格更新 | ✅ |
| **統計分析** | 月度收支、帳戶餘額 | ✅ |

### 🚧 規劃中（Phase 5-7）

- Phase 5: React 前端基礎
- Phase 6: 圖表與響應式設計
- Phase 7: Docker 部署、CI/CD、測試

---

## 🛠️ 技術棧

### 後端
- **Spring Boot 3.5.10** - 主框架
- **Spring Security 6** - 安全認證
- **Spring Data JPA** - ORM
- **PostgreSQL 15** - 主資料庫
- **Redis 7** - 快取層
- **WebSocket + STOMP** - 即時通訊
- **JWT (jjwt 0.12.5)** - Token 認證

### 外部 API
- **CoinGecko API** - 加密貨幣價格
- **Yahoo Finance API** - 股票價格

### 文檔與測試
- **Swagger/OpenAPI 3** - API 文檔
- **JUnit 5 + Mockito** - 單元測試

---

## 📦 專案結構
```
backend/
├── src/main/java/com/pocketfolio/backend/
│   ├── config/              # 配置類
│   │   ├── OpenApiConfig.java
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── SchedulerConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/          # REST 控制器
│   │   ├── AuthController.java
│   │   ├── TransactionController.java
│   │   ├── CategoryController.java
│   │   ├── AccountController.java
│   │   ├── AssetController.java
│   │   ├── PriceController.java
│   │   ├── PriceAlertController.java
│   │   ├── StatisticsController.java
│   │   └── AssetSnapshotController.java
│   ├── dto/                 # 數據傳輸對象
│   │   ├── request/         # 請求 DTO
│   │   ├── response/        # 回應 DTO
│   │   ├── websocket/       # WebSocket 訊息
│   │   └── external/        # 外部 API DTO
│   ├── entity/              # JPA 實體
│   │   ├── User.java
│   │   ├── Transaction.java
│   │   ├── Category.java
│   │   ├── Account.java
│   │   ├── Asset.java
│   │   ├── PriceAlert.java
│   │   └── AssetSnapshot.java
│   ├── repository/          # JPA Repository
│   ├── service/             # 業務邏輯
│   │   ├── external/        # 外部 API 服務
│   │   │   ├── CoinGeckoService.java
│   │   │   └── YahooFinanceService.java
│   │   ├── AuthService.java
│   │   ├── PriceService.java
│   │   ├── PriceAlertService.java
│   │   ├── AssetSnapshotService.java
│   │   └── WebSocketService.java
│   ├── security/            # 安全相關
│   │   ├── JwtUtil.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetailsService.java
│   │   └── SecurityUtil.java
│   ├── scheduler/           # 定時任務
│   │   └── PriceUpdateScheduler.java
│   └── exception/           # 異常處理
│       ├── GlobalExceptionHandler.java
│       └── ResourceNotFoundException.java
├── src/main/resources/
│   ├── application.yaml     # 應用配置
│   └── static/              # 靜態資源
│       └── ws-test.html     # WebSocket 測試頁面
└── src/test/                # 測試代碼

docker-compose.yaml          # Docker 服務配置
pom.xml                      # Maven 依賴管理
```

---

## 🚀 快速開始

### 前置需求

- **Java 17+**
- **Maven 3.8+**
- **Docker & Docker Compose**

### 1. 啟動資料庫服務
```bash
# 啟動 PostgreSQL + Redis
docker-compose up -d

# 檢查服務狀態
docker ps
```

### 2. 配置應用

編輯 `src/main/resources/application.yaml`：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pocketfolio
    username: admin
    password: password
  
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: your-secret-key-change-in-production
  expiration: 86400000  # 24 小時
```

### 3. 啟動後端
```bash
# 編譯並啟動
./mvnw spring-boot:run

# 或使用 IDE (IntelliJ IDEA / VS Code)
```

### 4. 訪問 API 文檔
```
Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 📚 API 概覽

### 認證 API（無需 Token）

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/auth/register` | 註冊新用戶 |
| POST | `/api/auth/login` | 用戶登入（取得 JWT） |

### 業務 API（需要 Bearer Token）

| 模組 | 端點數量 | 說明 |
|------|---------|------|
| 交易記錄 | 5 | POST, GET, GET/:id, PUT/:id, DELETE/:id |
| 類別管理 | 5 | POST, GET, GET/:id, PUT/:id, DELETE/:id |
| 帳戶管理 | 5 | POST, GET, GET/:id, PUT/:id, DELETE/:id |
| 資產管理 | 5 | POST, GET, GET/:id, PUT/:id, DELETE/:id |
| 價格管理 | 5 | 查詢價格、更新價格、清除快取 |
| 價格警報 | 6 | CRUD + 啟用/停用 |
| 統計分析 | 2 | 月度統計、帳戶餘額 |
| 資產快照 | 4 | 建立快照、查詢歷史趨勢 |

**總計：37 個 REST API**

完整 API 文檔請查看 Swagger UI。

---

## 🔐 認證流程

### 1. 註冊
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "使用者",
  "password": "password123"
}
```

### 2. 登入
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**回應：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": "...",
  "email": "user@example.com",
  "username": "使用者"
}
```

### 3. 使用 Token

所有業務 API 都需要在 Header 帶入 Token：
```http
Authorization: Bearer {token}
```

---

## 📊 資料模型

### 核心實體關係
```
User (用戶)
  ├── Transaction (交易記錄)
  ├── Category (類別)
  ├── Account (帳戶)
  │     └── Asset (資產持倉)
  ├── PriceAlert (價格警報)
  └── AssetSnapshot (資產快照)
```

### 資料隔離

- ✅ 每個用戶只能存取自己的資料
- ✅ Repository 層自動過濾 userId
- ✅ JWT Token 驗證身份

---

## ⚡ 定時任務

| 任務 | 執行時間 | 說明 |
|------|---------|------|
| 價格更新 | 每 5 分鐘 | 更新所有資產的即時價格 |
| 快照建立 | 每天凌晨 1 點 | 建立每日資產快照 |
| 快取清除 | 每天凌晨 3 點 | 清除過期的價格快取 |

---

## 🔌 WebSocket 即時推播

### 連接端點
```
ws://localhost:8080/ws
```

### 訂閱頻道
```javascript
// 價格更新（廣播）
stompClient.subscribe('/topic/price-updates', callback);

// 系統消息（廣播）
stompClient.subscribe('/topic/system', callback);

// 價格警報（個人）
stompClient.subscribe('/user/queue/alerts', callback);
```

### 測試頁面
```
http://localhost:8080/ws-test.html
```

---

## 🧪 測試

### 單元測試
```bash
# 執行所有測試
./mvnw test

# 執行特定測試
./mvnw test -Dtest=TransactionServiceTest
```

### API 測試

**方式 1：Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

**方式 2：REST Client**
```
使用 api-test.http（VS Code REST Client 插件）
或 IntelliJ IDEA HTTP Client
```

---

## 🐳 Docker 部署

### 開發環境
```bash
# 啟動資料庫服務
docker-compose up -d

# 啟動後端（本機）
./mvnw spring-boot:run
```

### 生產環境（Phase 7）

待補充...

---

## 📈 效能優化

### 已實作

- ✅ Redis 快取（價格資料 5 分鐘 TTL）
- ✅ @Cacheable 註解快取
- ✅ 資料庫索引（AssetSnapshot）
- ✅ 分頁查詢（避免一次載入大量資料）
- ✅ 懶加載（JPA Lazy Fetch）

### 待優化（Phase 7）

- ⬜ 資料庫連線池調優
- ⬜ API 限流
- ⬜ CDN 靜態資源

---

## 🔧 故障排除

### 問題 1：無法連接資料庫

**檢查：**
```bash
docker ps | grep postgres
psql -h localhost -U admin -d pocketfolio
```

### 問題 2：Redis 連線失敗

**檢查：**
```bash
docker exec -it pocketfolio-redis redis-cli ping
# 應回傳 PONG
```

### 問題 3：JWT Token 過期

**解決：** 重新登入取得新 Token

### 問題 4：WebSocket 連線失敗

**檢查：**
- CORS 設定（SecurityConfig）
- 防火牆是否阻擋 8080 port

---

## 📝 開發日誌

### Phase 1 (完成)
- ✅ Transaction CRUD
- ✅ DTO Pattern
- ✅ 全域異常處理
- ✅ 單元測試

### Phase 2 (完成)
- ✅ Category, Account, Asset CRUD
- ✅ 實體關聯設計
- ✅ 統計功能

### Phase 3 (完成)
- ✅ JWT 認證
- ✅ 資料權限隔離
- ✅ Spring Security 配置

### Phase 4 (完成)
- ✅ 外部 API 整合
- ✅ Redis 快取
- ✅ WebSocket 推播
- ✅ 價格警報
- ✅ 歷史快照

---

## 🤝 貢獻指南

（待補充）

---

## 📄 授權

MIT License

---

## 📧 聯絡方式

- Email: 91211wise@gmail.com
- GitHub: https://github.com/yourname/pocketfolio

---

## 🙏 致謝

- Spring Boot Team
- CoinGecko API
- Yahoo Finance API
- Redis Labs