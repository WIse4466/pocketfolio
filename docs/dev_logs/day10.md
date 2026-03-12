# Day 10 開發紀錄：資安上下文串接、JPA 效能優化與 SIT 整合測試

## 📝 本日開發重點摘要
今天完成了 Pocketfolio 記帳系統最核心的「多用戶資料隔離」機制，並透過 Spring Security 與 Spring Data JPA 的完美結合，確保系統的安全與效能。同時進行了完整的 API 系統整合測試 (SIT)，並排除了環境與例外處理的隱藏 Bug。

---

## 🛠️ 1. 核心開發與架構優化

### 1-1. 實作 SecurityUtil (全域安全上下文)
* **概念**：建立一個靜態工具類別 (Utility Class)，用於隨時獲取當下發送請求的使用者身分。
* **技術細節**：透過 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 提取保鑣驗證過的 `User` 實體。
* **資安意義**：系統從此「不再信任前端傳來的 userId」，全面改用 JWT 解析出的絕對真實 ID 來進行記帳與查詢，徹底防範 IDOR (越權存取) 漏洞。

### 1-2. JPA 效能優化：避免 N+1 查詢問題
* **實作**：在實體關聯 `@ManyToOne` 中強制加上 `fetch = FetchType.LAZY`（延遲載入）。
* **避坑指南**：為了避免轉譯 JSON 時觸發 `LazyInitializationException`（因為 Session 已關閉），確立了**「絕不將 Entity 直接回傳給前端，必須轉換為 DTO (Data Transfer Object)」**的架構鐵則。

### 1-3. 關聯儲存優化 (Foreign Key 設定)
* **實作技巧**：在新增 Transaction 時，不需要使用 `SELECT` 去撈出完整的 User 實體，而是透過 `new User()` 並 `setId()`（或使用 `getReferenceById()`）產生一個代理物件 (Proxy) 綁定關聯。
* **效益**：成功省下一次無意義的資料庫查詢，提升寫入效能。

### 1-4. 建立 Repository 查詢魔法與 JPQL
* **Method Derivation**：大量使用 `findByUserIdAnd...` 方法，確保所有查詢都加上 `userId` 濾網，達到多用戶資料隔離。
* **JPQL 應用**：撰寫客製化 `@Query`，利用 `CASE WHEN` 判斷收入/支出進行金額加總，並使用 `COALESCE` 防止 Null 錯誤。

---

## 🐛 2. 環境設定與 Debug 實戰

### 2-1. Mac 終端機 JAVA_HOME 迷路事件
* **狀況**：原生的 `zsh` 終端機無法執行 `./mvnw spring-boot:run`，顯示找不到 Java。
* **解決方案**：透過 `nano ~/.zshrc` 設定環境變數，將 Homebrew 安裝的 OpenJDK 21 路徑導出至 `JAVA_HOME` 與 `PATH` 中，並透過 `source ~/.zshrc` 生效。

### 2-2. 抓出「吃掉例外 (Exception Swallowing)」的公關主管
* **狀況**：測試「越權查詢交易」時，預期應回傳 `404 Not Found`，系統卻回傳冷冰冰的 `500 Internal Server Error`，且控制台沒有報錯訊息。
* **偵錯過程**：
    1. 發現 `GlobalExceptionHandler` 裡的 `Exception.class` 捕捉區塊沒有將錯誤印出。
    2. 補上 `ex.printStackTrace()` 強制印出紅字堆疊追蹤 (Stack Trace)。
    3. **真相大白**：發現錯誤其實是 `MethodArgumentTypeMismatchException`。原因是測試腳本中忘記將「替換成實際的UUID」字串替換為真實的 UUID，導致 Spring 無法將中文轉型為 UUID 而崩潰。
* **後續處理**：替換真實 UUID 後，成功觸發 `ResourceNotFoundException` 並完美回傳 404，驗證了資料隔離機制的有效性。

---

## 🎯 3. 系統整合測試 (SIT)
利用 IntelliJ 內建的 HTTP Client 執行 `api-test.http`，完成以下完整端到端 (E2E) 測試：
- [x] Alice 與 Bob 註冊與登入 (JWT 發放與驗證)
- [x] Alice 建立類別、帳戶與記帳明細
- [x] Bob 建立自己的資源
- [x] **資料隔離測試 (最核心)**：Bob 嘗試查詢、刪除、使用 Alice 的交易/類別/帳戶，均被系統成功阻擋 (回傳 404 或 400)。
- [x] 財務統計 API 驗證 (餘額計算與當月收支統計)

## 💡 明日目標
* 依據 SIT 測試經驗，將 API 測試腳本改寫為自動化測試腳本 (Spring Boot Test)，確保未來 CI/CD 流程的穩定性。
* 著手規劃前端介面與後端 API 的串接規格。