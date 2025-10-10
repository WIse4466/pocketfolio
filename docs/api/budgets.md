# Budgets API（本月總額與分類預算）

- 端點前綴：`/api/budgets`
- 說明：設定每月「總預算」與「分類預算」，並查詢本月花費與超支狀態。

## Summary（彙總）
GET `/api/budgets/summary?month=YYYY-MM`

回應（示例）：
```
{
  "month": "2025-10-01",
  "totalLimit": 10000.00,
  "totalSpent": 3450.00,
  "overspend": false,
  "categories": [
    { "categoryId": "...", "limit": 3000.00, "spent": 1200.00, "overspend": false }
  ]
}
```

備註：
- `month` 以該月第一天表示（例如 2025-10-01）。
- `totalSpent`、分類 `spent` 基於該月 EXPENSE 交易加總。

## 設定本月總預算（Upsert）
PUT `/api/budgets/total?month=YYYY-MM&limit=10000`

- 無回應內容（204）。

## 設定分類預算（Upsert）
PUT `/api/budgets/category?categoryId={UUID}&month=YYYY-MM&limit=3000`

- 無回應內容（204）。

