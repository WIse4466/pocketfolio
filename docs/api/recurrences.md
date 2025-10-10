# Recurrences API（固定週期交易：每月）

- 端點前綴：`/api/recurrences`
- 僅支援 INCOME/EXPENSE（排程在每日 00:05 TPE 實際產生交易）

## 列表
GET `/api/recurrences`

回應：陣列，含 `id/name/kind/amount/currencyCode/account{name,id,currencyCode}/category{id}/dayOfMonth/holidayPolicy/active`。

## 建立
POST `/api/recurrences`
```
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "name": "房租",
  "kind": "EXPENSE",
  "amount": 12000.00,
  "currencyCode": "TWD",
  "account": { "id": "..." },
  "category": { "id": "..." },
  "dayOfMonth": 1,              // 1-28；31 表示月末
  "holidayPolicy": "ADVANCE",   // NONE|ADVANCE|POSTPONE
  "active": true
}
```

## 啟用/停用
PUT `/api/recurrences/{id}/active?active=true|false`

回應：該筆排程的 DTO。

## 手動執行今日
POST `/api/recurrences/run-today?date=YYYY-MM-DD`

- 無回應內容（204）。
- 於指定 `date` 判定是否應排程於該日，若是則建立交易。

