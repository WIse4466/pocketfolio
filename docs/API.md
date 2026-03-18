# PocketFolio API 參考手冊

> 完整的 API 文檔請訪問：http://localhost:8080/swagger-ui.html

---

## 🔐 認證

所有 API（除了 `/api/auth/**`）都需要 JWT Token。

**Header 格式：**
```
Authorization: Bearer {token}
```

---

## 📋 API 清單

### 1. 認證 API

| Method | Endpoint | 說明 | 需要 Token |
|--------|----------|------|-----------|
| POST | `/api/auth/register` | 註冊 | ❌ |
| POST | `/api/auth/login` | 登入 | ❌ |

---

### 2. 交易記錄 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| POST | `/api/transactions` | 建立交易 | body: TransactionRequest |
| GET | `/api/transactions` | 查詢列表 | ?categoryId, ?accountId, ?startDate, ?endDate |
| GET | `/api/transactions/{id}` | 查詢單筆 | id: UUID |
| PUT | `/api/transactions/{id}` | 更新交易 | id: UUID, body: TransactionRequest |
| DELETE | `/api/transactions/{id}` | 刪除交易 | id: UUID |

---

### 3. 類別管理 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| POST | `/api/categories` | 建立類別 | body: CategoryRequest |
| GET | `/api/categories` | 查詢列表 | ?type=INCOME/EXPENSE |
| GET | `/api/categories/{id}` | 查詢單筆 | id: UUID |
| PUT | `/api/categories/{id}` | 更新類別 | id: UUID, body: CategoryRequest |
| DELETE | `/api/categories/{id}` | 刪除類別 | id: UUID |

---

### 4. 帳戶管理 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| POST | `/api/accounts` | 建立帳戶 | body: AccountRequest |
| GET | `/api/accounts` | 查詢列表 | ?type, ?search |
| GET | `/api/accounts/{id}` | 查詢單筆 | id: UUID |
| PUT | `/api/accounts/{id}` | 更新帳戶 | id: UUID, body: AccountRequest |
| DELETE | `/api/accounts/{id}` | 刪除帳戶 | id: UUID |

---

### 5. 資產管理 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| POST | `/api/assets` | 建立資產 | body: AssetRequest |
| GET | `/api/assets/account/{accountId}` | 查詢帳戶資產 | accountId: UUID, ?type |
| GET | `/api/assets/{id}` | 查詢單筆 | id: UUID |
| PUT | `/api/assets/{id}` | 更新資產 | id: UUID, body: AssetRequest |
| DELETE | `/api/assets/{id}` | 刪除資產 | id: UUID |

---

### 6. 價格管理 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| GET | `/api/prices/{symbol}` | 查詢即時價格 | symbol: String, type: AssetType |
| POST | `/api/prices/update/asset/{id}` | 更新資產價格 | id: UUID |
| POST | `/api/prices/update/my-assets` | 批次更新 | - |
| DELETE | `/api/prices/cache` | 清除所有快取 | - |
| DELETE | `/api/prices/cache/{symbol}` | 清除特定快取 | symbol: String, type: AssetType |

---

### 7. 價格警報 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| POST | `/api/price-alerts` | 建立警報 | body: PriceAlertRequest |
| GET | `/api/price-alerts` | 查詢列表 | ?activeOnly=true/false |
| GET | `/api/price-alerts/{id}` | 查詢單筆 | id: UUID |
| PUT | `/api/price-alerts/{id}` | 更新警報 | id: UUID, body: PriceAlertRequest |
| PATCH | `/api/price-alerts/{id}/toggle` | 啟用/停用 | id: UUID, active: boolean |
| DELETE | `/api/price-alerts/{id}` | 刪除警報 | id: UUID |

---

### 8. 統計分析 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| GET | `/api/statistics/monthly/{year}/{month}` | 月度統計 | year: int, month: int |
| GET | `/api/statistics/account-balances` | 帳戶餘額總覽 | - |

---

### 9. 資產快照 API

| Method | Endpoint | 說明 | 參數 |
|--------|----------|------|------|
| POST | `/api/snapshots/asset/{id}` | 建立快照 | id: UUID |
| GET | `/api/snapshots/asset/{id}/history` | 資產歷史 | id: UUID, ?days=30 |
| GET | `/api/snapshots/portfolio/{date}` | 投資組合快照 | date: YYYY-MM-DD |
| GET | `/api/snapshots/portfolio/history` | 投資組合趨勢 | ?days=30 |

---

## 📊 常用請求範例

### 註冊用戶
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "使用者",
  "password": "password123"
}
```

### 建立交易
```http
POST /api/transactions
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 100,
  "note": "午餐",
  "date": "2026-03-01",
  "categoryId": "uuid",
  "accountId": "uuid"
}
```

### 查詢 BTC 價格
```http
GET /api/prices/BTC?type=CRYPTO
Authorization: Bearer {token}
```

### 建立價格警報
```http
POST /api/price-alerts
Authorization: Bearer {token}
Content-Type: application/json

{
  "symbol": "BTC",
  "assetType": "CRYPTO",
  "condition": "BELOW",
  "targetPrice": 60000
}
```