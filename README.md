# 💰 PocketFolio

個人財務管理系統，支援收支記帳、帳戶管理，以及股票與加密貨幣資產即時追蹤。

---

## 🛠️ 技術棧

| 層級 | 技術 |
|------|------|
| 後端 | Java 17 · Spring Boot 3 · Spring Data JPA · Spring Security |
| 資料庫 | PostgreSQL 15 |
| 前端 | React 18 · TypeScript · Tailwind CSS |
| 容器化 | Docker · Docker Compose |

---

## 📁 專案結構

```
pocketfolio/
├── backend/                  # Spring Boot 後端
│   ├── src/
│   │   └── main/java/com/pocketfolio/backend/
│   │       ├── controller/   # REST API 控制器
│   │       ├── service/      # 業務邏輯
│   │       ├── repository/   # 資料存取層
│   │       ├── entity/       # JPA 實體
│   │       ├── dto/          # 資料傳輸物件
│   │       └── exception/    # 例外處理
│   ├── src/main/resources/
│   │   └── application.yaml  # 應用程式設定
│   ├── docker-compose.yaml   # 資料庫容器設定
│   └── pom.xml
│
└── frontend/                 # React 前端（規劃中）
    ├── src/
    └── package.json
```

---

## 🚀 快速啟動

### 前置需求

請確認已安裝以下工具：

- [Java 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Node.js 18+](https://nodejs.org/)（前端開發用）

---

### Step 1 — 啟動資料庫

在 `backend/` 目錄下執行：

```bash
docker-compose up -d
```

確認資料庫正常啟動：

```bash
docker ps
# 應看到 pocketfolio-db 狀態為 Up
```

> **資料庫連線資訊**
> - Host: `localhost:5432`
> - Database: `pocketfolio`
> - Username: `admin`
> - Password: `password`

---

### Step 2 — 啟動後端

在 `backend/` 目錄下執行：

```bash
./mvnw spring-boot:run
```

或使用 IDE（IntelliJ / VS Code）直接執行 `BackendApplication.java`。

啟動成功後可看到：

```
Started BackendApplication in X.XXX seconds
```

後端預設運行於 **http://localhost:8080**

---

### Step 3 — 驗證後端正常

```bash
# 建立一筆測試交易
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000, "note": "測試", "date": "2026-02-15"}'

# 預期回應：201 Created + 交易資料 JSON
```

---

### Step 4 — 啟動前端（規劃中）

```bash
cd frontend
npm install
npm run dev
```

前端預設運行於 **http://localhost:5173**

---

## 📡 API 文件

### Transaction（交易）

| 方法 | 路徑 | 說明 |
|------|------|------|
| `POST` | `/api/transactions` | 建立交易 |
| `GET` | `/api/transactions` | 查詢列表（支援分頁） |
| `GET` | `/api/transactions/{id}` | 查詢單筆 |
| `PUT` | `/api/transactions/{id}` | 更新交易 |
| `DELETE` | `/api/transactions/{id}` | 刪除交易 |

#### 分頁查詢參數

```
GET /api/transactions?page=0&size=10&sort=date,desc
```

| 參數 | 說明 | 預設值 |
|------|------|--------|
| `page` | 頁碼（從 0 開始）| `0` |
| `size` | 每頁筆數 | `10` |
| `sort` | 排序欄位與方向 | `date,desc` |

#### Request Body 範例

```json
{
  "amount": 1000.00,
  "note": "二月薪資",
  "date": "2026-02-15"
}
```

#### 錯誤回應格式

```json
{
  "timestamp": "2026-02-15T10:00:00",
  "message": "找不到 ID 為 xxx 的交易"
}
```

---

## 🧪 執行測試

```bash
./mvnw test
```

---

## 🛑 停止所有服務

```bash
# 停止資料庫容器
docker-compose down

# 停止並清除資料（慎用）
docker-compose down -v
```

---

## 📋 開發進度

- [x] Transaction CRUD API
- [x] 資料驗證與統一錯誤處理
- [ ] Category（類別）
- [ ] Account（帳戶）
- [ ] 用戶系統（Spring Security + JWT）
- [ ] 資產追蹤（股票 / 加密貨幣即時報價）
- [ ] 前端 React 介面

---

## 🤝 貢獻指南

1. Fork 此專案
2. 建立 Feature Branch：`git checkout -b feature/your-feature`
3. Commit 變更：`git commit -m 'feat: add your feature'`
4. Push 到 Branch：`git push origin feature/your-feature`
5. 開 Pull Request