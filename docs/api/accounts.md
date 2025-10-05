# Accounts API — due fields

本節補充帳戶（Accounts）的新增欄位與規則：

新增欄位（V4）
- `dueMonthOffset: 0|1|2`（0=本月, 1=下月(預設), 2=下下月）
- `dueHolidayPolicy: "NONE"|"ADVANCE"|"POSTPONE"`（繳款日遇假日的調整策略）
- `autopayEnabled: boolean`（是否啟用自動扣款）

注意：上述欄位僅對 `type=CREDIT_CARD` 有意義。其他類型提供亦會保存預設值但不產生效果。

錯誤碼（BusinessException.code）
- `DUE_MONTH_OFFSET_INVALID`：`dueMonthOffset` 必須為 0/1/2
- `DUE_HOLIDAY_POLICY_INVALID`：`dueHolidayPolicy` 必須為 `NONE|ADVANCE|POSTPONE`
- `AUTOPAY_NOT_SUPPORTED`：非信用卡帳戶不可啟用自動扣款
- `AUTOPAY_ACCOUNT_INVALID`：自動扣款帳戶不可為信用卡
- `AUTOPAY_CONFLICT`：`autopayEnabled=false` 但提供了 `autopayAccount`

更新請求（示例）
```
PUT /api/accounts/{id}
Content-Type: application/json

{
  "name": "My Card",
  "type": "CREDIT_CARD",
  "currencyCode": "TWD",
  "dueMonthOffset": 1,
  "dueHolidayPolicy": "ADVANCE",
  "autopayEnabled": true,
  "autopayAccount": { "id": "11111111-1111-1111-1111-111111111111" },
  "notes": "台新@GoGo"
}
```

回應格式同原本 Account 實體，將包含新欄位。

