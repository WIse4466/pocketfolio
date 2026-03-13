# Day 11 開發紀錄：現代化微服務升級 - Redis、排程器與外部 API 串接準備

## 📝 本日開發重點摘要
系統架構全面升級！本日為 Pocketfolio 導入了與外部世界溝通的能力，並建置了 Redis 快取伺服器以應付未來的高併發查詢。同時，完成了金融報價 API 的 DTO（資料傳輸物件）設計，展現了嚴謹的防禦性編程思維。

---

## 🏗️ 1. 基礎建設升級 (Infrastructure)

### 1-1. 核心依賴 (Dependencies) 導入
系統正式引入四大現代化微服務套件：
* **`spring-boot-starter-webflux`**：提供非同步非阻塞的 `WebClient`，負責高效能呼叫外部 API。
* **`spring-boot-starter-data-redis`**：引入 Redis 記憶體資料庫支援，作為系統高速快取層。
* **`spring-boot-starter-websocket`**：為未來的「即時股價推播」雙向通訊預作準備。
* **`jackson-databind`**：強化 JSON 序列化與反序列化能力。

### 1-2. Docker 容器化部署 Redis
* 於 `docker-compose.yaml` 新增 `redis:7-alpine` 輕量化映像檔。
* 開啟 `--appendonly yes` (AOF, Append Only File) 實現資料持久化，防止斷電重啟造成快取遺失。
* 排除本機 Mac `brew services` 佔用 `6379` Port 的衝突問題，成功連線並取得 `PONG` 回應。

---

## ⚙️ 2. 核心架構與設定 (Configuration)

### 2-1. Spring 設定檔宇宙觀釐清
釐清了 Spring Boot 底層架構的設計哲學：
* **`spring.datasource`**：專屬於走 JDBC 協定、聽得懂 SQL 的傳統關聯式資料庫（如 PostgreSQL）。
* **`spring.data.*`**：隸屬於 Spring Data 計畫，專門容納 NoSQL 新型態資料庫（如 Redis, MongoDB），兩者在底層連線池與運作機制上完全不同。

### 2-2. 系統藍圖配置 (`application.yml`)
* **排程器配置**：加入 `scheduler.price-update` 設定，並釐清 `@Scheduled` 中 `cron` 與 `initial-delay` 不能混用的 Spring 底層限制。
* **環境變數抽離**：將 CoinGecko 與 Yahoo Finance 的 API 網址抽離為自訂變數，避免 Hardcode，提升部署彈性。

### 2-3. Redis 序列化配置 (`RedisConfig.java`)
* 自訂 `RedisTemplate`，將 Key 序列化改為 `StringRedisSerializer`，Value 改為 `GenericJackson2JsonRedisSerializer`，解決預設儲存會產生外星文亂碼（位元組字串）的問題，提升除錯可讀性。

---

## 🛡️ 3. 外部 API 資料對接設計 (DTO Design Patterns)

本日實作了三張關鍵的 DTO 報關單，展示了高度防禦性的設計模式：

### 3-1. 外部供應商 DTO (CoinGecko & Yahoo Finance)
* **未知屬性防護罩**：全面加上 `@JsonIgnoreProperties(ignoreUnknown = true)`，防止外部 API 偷偷新增欄位導致 Jackson 翻譯崩潰。
* **命名風格轉換**：使用 `@JsonProperty` 完美橋接 JSON 的 `snake_case` 與 Java 的 `camelCase`。
* **拆解俄羅斯娃娃結構**：利用靜態內部類別 (`public static class`) 優雅地對應 Yahoo API 惡名昭彰的深層巢狀 JSON。
* **金融級精準度**：所有金額欄位全面強制使用 `BigDecimal`，徹底杜絕浮點數運算的精度流失。
* **NPE 防雷針**：自訂 `getPrice()` 方法封裝複雜的取值與 null 判斷邏輯，保護 Service 層不被 `NullPointerException` 攻擊。

### 3-2. 內部對外輸出 DTO (`PriceUpdateResponse`)
* **建造者模式**：引入 Lombok `@Builder`，讓擁有 7 個屬性的物件建立過程變得直覺且優雅。
* **優雅降級 (Graceful Degradation)**：設計 `success` 與 `errorMessage` 欄位，確保批次更新股價時，單一股票查詢失敗不會導致整個系統崩潰。
* **前端友善設計**：預先計算 `changePercent` (漲跌幅) 並附上 `updateTime`，大幅降低前端的計算負擔，提供更可靠的資料標記。

---

## 💡 明日目標
* 實作 `WebClientConfig`，並撰寫 `MarketPriceService` 實際發送 HTTP 請求串接 CoinGecko 或 Yahoo Finance。
* 將抓取到的即時報價寫入 Redis 快取。
* 啟動 `@EnableScheduling`，讓系統開始自動化輪詢金融市場資料。