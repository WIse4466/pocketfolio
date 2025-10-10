CSV Importer — 天天記帳資料(收支).csv

簡介
- 讀取你提供的「天天記帳資料(收支).csv」，逐筆建立 Pocketfolio 的交易（INCOME/EXPENSE）。
- 自動比對「帳戶名稱」與現有帳戶；可選擇自動建立不存在的帳戶/分類。
- 預設以台北時間當天 00:00 產生交易時間（避免跨日問題）。

安裝
1) 進入資料夾並安裝依賴
   cd scripts/ttjb-import
   npm ci

2) 確認後端 API 已啟動（http://localhost:8080）

使用方式
node index.mjs --file "/完整路徑/天天記帳資料(收支).csv" [--api http://localhost:8080] [--user 00000000-0000-0000-0000-000000000001] [--create-accounts] [--create-categories]

參數
- --file：CSV 檔案路徑（可含空白與中文）
- --api：後端 API Base（預設 http://localhost:8080）
- --user：導入使用者 ID（預設預設使用者）
- --create-accounts：若 CSV 帳戶在系統中不存在，自動建立（type=CASH, currency=TWD, 起始餘額 0）
- --create-categories：若 CSV 分類不存在，建立頂層分類（僅建立單層；含子分類時需先行建立）

支援的欄位（表頭）
- 日期：日期/Date
- 類型（可選）：類型/Type（值：收入/支出/INCOME/EXPENSE）
- 金額：金額/Amount（或使用 收入/支出 兩欄擇一有值）
- 收入（可選）：收入/Income
- 支出（可選）：支出/Expense
- 帳戶：帳戶/Account
- 分類（可選）：分類/Category
- 子分類（可選）：子分類/Subcategory
- 幣別（可選）：幣別/Currency（未提供時取帳戶幣別）
- 備註（可選）：備註/Notes

匯入邏輯摘要
- kind 判定：
  - 若「類型」有值，映射為 INCOME/EXPENSE；
  - 否則以「收入/支出」兩欄中非空的數值決定。
- 金額：先取「金額」，無則取收入/支出欄的非空值。
- 帳戶：以名稱比對現有帳戶；若啟用 --create-accounts 則自動建立。
- 分類：以「分類/子分類」比對；若啟用 --create-categories 且僅單層分類時自動建立。
- 時間：以台北時間當天 00:00 轉為 UTC 寫入。

注意事項
- 目前僅導入 INCOME/EXPENSE；若 CSV 有轉帳請排除或另行處理。
- 多幣別：後端 MVP 僅單幣別；幣別不一致將被後端拒絕。
- 請先建立必要的帳戶/分類，或使用 --create-* 參數協助建立。

範例
node index.mjs --file "/Users/you/Downloads/天天記帳資料(收支).csv" --create-accounts --create-categories

