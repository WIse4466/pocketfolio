# Day 12 開發紀錄：外部 API 串接、全自動排程與企業級架構重構

## 📝 本日開發重點摘要
今天完成了 Pocketfolio 最核心的「資產報價自動化更新系統」。透過 `WebClient` 非同步呼叫外部 API，結合 `Spring Scheduler` 實現背景定時更新。更重要的是，在實作過程中進行了深度的架構重構，排除了 OOM (記憶體溢出)、AOP 代理失效、以及 Jackson 序列化等重大系統隱患，達到 Production-Ready（正式上線）的標準。

---

## 🏗️ 1. 核心模組實作 (Core Features)

### 1-1. 外部 API 串接 (WebClient)
* 實作 `CoinGeckoService` 與 `YahooFinanceService`，利用 Spring WebFlux 的 `WebClient` 發送 HTTP GET 請求。
* 成功處理 `Mono` 非同步響應，並轉換為內部標準的 `PriceData` DTO。
* 加入 `Thread.sleep(500)` 作為限流機制 (Rate Limiting)，防止頻繁呼叫導致伺服器 IP 被外部 API 封鎖。

### 1-2. 自動化排程 (Spring Scheduler)
* 實作 `PriceUpdateScheduler`，並使用 `@ConditionalOnProperty` 作為環境開關，確保本機開發時不會誤觸真實 API。
* 設定 Cron Job 達成「每 5 分鐘全站資產更新」與「每日凌晨 3 點強制清空快取」的自動化維護任務。

### 1-3. 價格管理 API (Controller)
* 建立 `PriceController`，提供前端 `GET` 查詢與 `POST` 強制更新的 RESTful 端點。
* 嚴格落實資安：`updateMyAssetPrices` API 不信任前端傳入的 ID，直接從 JWT Context (`SecurityUtil`) 提取 UserID，防止 IDOR 越權漏洞。

---

## 🛡️ 2. 企業級架構優化與拆彈紀錄 (Architecture & Debugging)

本日移除了數個會導致伺服器崩潰的隱患，並進行了深度的架構重構：

### 💣 拆彈 1：Spring AOP 代理失效 (Proxy Bypass)
* **問題**：在 Service 內部直接呼叫帶有 `@CacheEvict` 的方法，會繞過 Spring 的動態代理，導致 Redis 快取無法被清除。
* **解法**：砍掉多餘的內部呼叫方法，直接使用強大的 **SpEL (Spring Expression Language)** 動態生成 Key：`key = "(#assetType.name() == 'CRYPTO' ? 'crypto:' : 'stock:') + #symbol.toUpperCase()"`，完美觸發 AOP 攔截。

### 💣 拆彈 2：資料庫 OOM 記憶體核彈
* **問題**：在排程更新所有資產時，原使用 `findAll().stream().filter(...)`，這會將整張資料表載入記憶體，導致資料量大時直接觸發 OutOfMemory 當機。
* **解法**：
    1. **單一用戶更新**：在 Repository 層新增 `findByUserId(UUID)`，將過濾工作交還給資料庫底層 SQL。
    2. **全站排程更新**：導入 Spring Data JPA 的 `PageRequest` 進行 **分批處理 (Chunking/Pagination)**，每次僅從資料庫拉取 100 筆資料，確保伺服器記憶體水位永遠保持平穩。

### 💣 拆彈 3：Jackson 翻譯官污染 (ObjectMapper Scope)
* **問題**：為了 Redis 配置而註冊的 `ObjectMapper` `@Bean`（開啟了 DefaultTyping 記錄型別），污染了全域的 Web API 解析，導致前端登入時因缺少 `@class` 屬性而引發 HTTP 500 錯誤。
* **解法**：撤銷該設定的 `@Bean` 標記，改為私有方法 `redisObjectMapper()` 專供 Redis Serializer 使用，成功將內部快取解析與外部 API 解析隔離。

### 💣 拆彈 4：Jackson 自作聰明的虛擬屬性
* **問題**：DTO 中的 `isExpired()` 邏輯方法被 Jackson 誤認為是名為 `expired` 的實體屬性，並存入 Redis，導致讀取反序列化時崩潰 (`UnrecognizedPropertyException`)。
* **解法**：在方法上標註 `@JsonIgnore`，並在類別加上 `@JsonIgnoreProperties(ignoreUnknown = true)` 防護罩。

### 💣 拆彈 5：無腦休眠與假時間標籤
* **問題**：原先將 `@Cacheable` 加在底層，但將 `Thread.sleep` 與 `updateTime(now)` 寫在呼叫端，導致即使命中快取仍會強制休眠，且回傳錯誤的「最新」時間。
* **解法**：實施**「權責下放」**，將 `@Cacheable`、時間戳記封裝 (`PriceData`)、以及限流休眠，全部移至 `CoinGeckoService` 與 `YahooFinanceService` 內。達成**「真出門才休眠，拿快取秒回傳」**的高效能架構。

---

## 💡 明日目標
* 完成前端介面 (Frontend) 的資產新增與更新串接。
* 撰寫價格更新與排程服務的 Unit Test 確保邏輯堅不可摧。
* 準備實作「歷史資產價值趨勢 (Portfolio History)」的紀錄功能，畫出資產走勢圖。