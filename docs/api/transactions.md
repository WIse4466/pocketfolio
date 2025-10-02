# Transactions API（MVP）

建立「收入 / 支出 / 轉帳」交易，並同步更新帳戶餘額（MVP：直接更新 `current_balance`）。

- 端點：`POST /api/transactions`
- `Content-Type: application/json`

## 請求欄位

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

## 回應

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

## 錯誤格式

`400 Bad Request` 等：
```json
{ "error": "ValidationError", "message": "..." }
```

## 商業規則（MVP）

- INCOME：帳戶餘額 `+ amount`
- EXPENSE：帳戶餘額 `- amount`
- TRANSFER：來源 `- amount`、目標 `+ amount`
- 帳戶不可為 `archived`
- 僅支援單幣別；跨幣交易會被拒絕（400）
- 轉帳來源與目標不可相同

## 之後（非 MVP）

- `GET /api/transactions?from=&to=` 取得區間交易
- idempotency key 防重複提交
- 多幣別與匯率、信用卡結帳/扣款規則

