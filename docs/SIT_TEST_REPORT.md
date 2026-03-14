# Pocketfolio 系統整合測試報告 (SIT Report)

## 測試環境
- **時間**: 2026-03
- **測試目標**: 身分驗證 (JWT) 與多用戶資料隔離機制 (Data Isolation)
- **測試工具**: VS Code HTTP Client (`api-test.http`)

## 測試案例矩陣 (Test Case Matrix)

| Test ID | 測試情境 (Scenario) | 測試步驟 (Steps) | 預期結果 (Expected) | 實際結果 (Actual) | 狀態 |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **AUTH-01** | Alice 註冊新帳號 | 發送 POST `/api/auth/register` (Alice) | HTTP 200，回傳 JWT Token | HTTP 200，成功取得 Token | ✅ Pass |
| **AUTH-02** | Bob 註冊新帳號 | 發送 POST `/api/auth/register` (Bob) | HTTP 200，回傳 JWT Token | HTTP 200，成功取得 Token | ✅ Pass |
| **DATA-01** | Alice 建立交易明細 | Alice 帶 Token 發送 POST 建立一筆交易 | HTTP 201，資料庫新增一筆 Alice 的交易 | HTTP 201，成功建立 | ✅ Pass |
| **SEC-01** | Bob 查詢自己的交易 | Bob 帶 Token 發送 GET 查詢交易列表 | HTTP 200，回傳空陣列 (Bob 尚未記帳) | HTTP 200，回傳 `[]` | ✅ Pass |
| **SEC-02** | Bob 越權查詢 Alice 的交易 | Bob 帶 Token 發送 GET 查詢 Alice 的特定 Transaction ID | HTTP 403 / 404，拒絕存取 | HTTP 404，找不到該資源 | ✅ Pass |
| **SEC-03** | 未登入存取受保護 API | 不帶 Token 發送 GET 查詢交易列表 | HTTP 401 Unauthorized | HTTP 401，顯示「請重新登入」 | ✅ Pass |

## 測試環境
- **時間**: 2026-03-14
- **測試目標**: 外部API查詢，Cache機制實作。
- **測試工具**: VS Code HTTP Client (`api-test.http`)

## 測試案例矩陣 (Test Case Matrix) - 報價與快取系統 (Price API)

| Test ID | 測試情境 (Scenario) | 測試步驟 (Steps) | 預期結果 (Expected) | 實際結果 (Actual) | 狀態 |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **PRICE-01** | 查詢即時加密貨幣價格 | 帶 Token 發送 GET `/api/prices/BTC?type=CRYPTO` | HTTP 200，回傳 `PriceData` 且來源為 CoinGecko | HTTP 200，成功取得最新報價 | ✅ Pass |
| **PRICE-02** | 查詢即時股票價格 | 帶 Token 發送 GET `/api/prices/2330.TW?type=STOCK` | HTTP 200，回傳 `PriceData` 且來源為 Yahoo Finance | HTTP 200，成功取得最新報價 | ✅ Pass |
| **CACHE-01** | Redis 快取命中測試 | 連續兩次發送 GET 查詢 BTC 價格 | 第一次觸發外部 API，第二次毫秒級回傳且 Console 無 API 呼叫 Log | 第二次查詢成功從 Redis 快取讀取 | ✅ Pass |
| **CACHE-02** | 清除特定資產快取 | 帶 Token 發送 DELETE `/api/prices/cache/BTC?type=CRYPTO` | HTTP 200，回傳「快取已清除: BTC」 | HTTP 200，成功清除精準 Key 快取 | ✅ Pass |
| **CACHE-03** | 清除所有價格快取 | 帶 Token 發送 DELETE `/api/prices/cache` | HTTP 200，回傳「所有價格快取已清除」 | HTTP 200，成功全域清除 Redis 快取 | ✅ Pass |

## 測試案例矩陣 (Test Case Matrix) - 完整價格更新業務流程 (Workflow)

| Test ID | 測試情境 (Scenario) | 測試步驟 (Steps) | 預期結果 (Expected) | 實際結果 (Actual) | 狀態 |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **WF-01** | 建立投資組合基礎資料 | Alice 建立「投資帳戶」，並新增「BTC」與「ETH」兩筆資產 | HTTP 201，資料庫成功建立關聯資料 | HTTP 201，成功取得 Account ID 與 Asset ID | ✅ Pass |
| **WF-02** | 手動更新單一資產價格 | 帶 Token 發送 POST `/api/prices/update/asset/{assetId}` | HTTP 200，回傳該資產的 `oldPrice`, `newPrice`, `changePercent` | HTTP 200，資料庫價格更新成功 | ✅ Pass |
| **WF-03** | 驗證單一資產損益計算 | 價格更新後，發送 GET `/api/assets/{assetId}` 查詢 | HTTP 200，`currentPrice`, `profitLoss`, `profitLossPercent` 依據新價重新計算 | HTTP 200，損益計算精準無誤 | ✅ Pass |
| **WF-04** | 批次更新我的所有資產 | 帶 Token 發送 POST `/api/prices/update/my-assets` | HTTP 200，回傳陣列包含 BTC 與 ETH 的更新結果，且觸發限流休眠 (500ms) | HTTP 200，所有資產價格皆完成更新 | ✅ Pass |
| **WF-05** | 驗證帳戶總餘額連動 | 批次更新後，發送 GET 查詢該「投資帳戶」狀態 | HTTP 200，`currentBalance` 等於 BTC 總市值 + ETH 總市值 | HTTP 200，帳戶總餘額正確反映最新市值 | ✅ Pass |