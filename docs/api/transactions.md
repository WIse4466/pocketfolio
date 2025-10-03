# Transactions API（MVP）

建立「收入 / 支出 / 轉帳」交易，查詢區間交易，刪除交易（會回沖餘額）。MVP 直接更新 `current_balance`。

- 端點：`POST /api/transactions`
- `Content-Type: application/json`

## 建立交易（POST）

端點：`POST /api/transactions`

### 請求欄位

通用：
- `userId: UUID`（必填）
- `kind: "INCOME" | "EXPENSE" | "TRANSFER"`（必填）
- `amount: number (> 0)`（必填）
- `occurredAt: ISO8601`（必填）
- `notes?: string`
- `currencyCode: string(3)`（必填；MVP 僅支援單幣別）
- `fxRateUsed?: number`（預留）

INCOME / EXPENSE：
- `accountId: UUID`（必填）
- `categoryId?: UUID`

TRANSFER：
- `sourceAccountId: UUID`（必填）
- `targetAccountId: UUID`（必填；且不可與 `sourceAccountId` 相同）

### 回應

`201 Created`，回傳建立完成的交易摘要：
```json
{
  "id": "UUID",
  "userId": "UUID",
  "kind": "INCOME",
  "amount": 1000.00,
  "occurredAt": "2025-01-01T00:00:00Z",
  "accountId": "UUID",
  "sourceAccountId": null,
  "targetAccountId": null,
  "categoryId": "UUID",
  "notes": "...",
  "currencyCode": "TWD",
  "fxRateUsed": null
}
```

### 錯誤格式

`400 Bad Request` 等：
```json
{ "error": "ValidationError", "message": "..." }
```

### 商業規則（MVP）

- INCOME：帳戶餘額 `+ amount`
- EXPENSE：帳戶餘額 `- amount`
- TRANSFER：來源 `- amount`、目標 `+ amount`
- 帳戶不可為 `archived`
- 僅支援單幣別；跨幣交易會被拒絕（400）
- 轉帳來源與目標不可相同

---

## 查詢交易（GET）

端點：`GET /api/transactions`

用於依期間載入交易（支援分頁）。回應為 Spring Page 格式。

查詢參數：
- `userId?: UUID`（未提供時預設為種子使用者 `00000000-0000-0000-0000-000000000001`）
- `from?: ISO8601`（起始時間，預設 `Instant.EPOCH`）
- `to?: ISO8601`（結束時間，預設現在）
- `page?: number` 預設 0
- `size?: number` 預設 50

範例：
```
GET /api/transactions?from=2025-01-01T00:00:00Z&to=2025-02-01T00:00:00Z&page=0&size=200
```

回應（節錄）：
```json
{
  "content": [
    {
      "id": "...",
      "userId": "...",
      "kind": "INCOME",
      "amount": 1000.0,
      "occurredAt": "2025-01-05T00:00:00Z",
      "accountId": "...",
      "sourceAccountId": null,
      "targetAccountId": null,
      "categoryId": "...",
      "notes": "...",
      "currencyCode": "TWD",
      "fxRateUsed": null
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 200 },
  "totalElements": 3,
  "totalPages": 1
}
```

錯誤格式同上（`{ error, message }`）。

---

## 刪除交易（DELETE）

端點：`DELETE /api/transactions/{id}`

用途：刪除單筆交易，並回沖餘額。

行為：
- INCOME：帳戶餘額會減少相同金額
- EXPENSE：帳戶餘額會增加相同金額
- TRANSFER：來源帳戶餘額增加、目標帳戶餘額減少

回應：
- `204 No Content` 刪除成功
- `404 NotFound` 找不到該交易：`{ "error": "NotFound", "message": "..." }`

範例：
```
DELETE /api/transactions/4a0e53dc-a0bb-4bd0-b0ab-99b5f8a04f84
```

---

## 之後（非 MVP）

- `GET /api/transactions?from=&to=` 取得區間交易
- idempotency key 防重複提交
- 多幣別與匯率、信用卡結帳/扣款規則
