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