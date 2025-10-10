# FX API（每日匯率與淨值換算）

- 端點前綴：`/api/fx`
- 基準幣別：預設 `TWD`

## 查詢某日匯率
GET `/api/fx/rates?date=YYYY-MM-DD&base=TWD`

回應（示例）：
```
[
  { "quote": "USD", "rate": 32.500000 },
  { "quote": "JPY", "rate": 0.220000 }
]
```

## 新增/更新匯率
PUT `/api/fx/rates?date=YYYY-MM-DD&base=TWD&quote=USD&rate=32.5`

- 無回應內容（204）。

## 淨值（基於匯率）
GET `/api/fx/net-worth?date=YYYY-MM-DD&base=TWD`

回應（示例）：
```
{
  "base": "TWD",
  "date": "2025-10-10",
  "netWorthTwd": 123456.78,
  "items": [
    { "accountId": "...", "name": "My Bank", "currency": "TWD", "balance": 10000.00, "twd": 10000.00 },
    { "accountId": "...", "name": "USD Cash", "currency": "USD", "balance": 100.00, "twd": 3250.00 }
  ],
  "rates": { "TWD": 1, "USD": 32.5 }
}
```

