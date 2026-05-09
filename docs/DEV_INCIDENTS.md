# PocketFolio 開發事件紀錄

> 記錄開發過程中遇到的問題、決策與解法。每一筆都是真實踩過的坑，是這個專案最有價值的部分。
>
> 更新時間：2026-05-09

---

## 格式說明

| 欄位 | 說明 |
|---|---|
| 日期 | 對應 dev_log 的 day 編號 |
| 分類 | Bug / 架構設計 / 效能 / 資安 / 部署 / 工具 |
| 標題 | 一句話說明問題或決策 |
| 詳情 | 問題根因或設計背景 |
| 解決辦法 | 具體怎麼解的 |

---

## 事件總表

| 日期 | 分類 | 標題 | 詳情 | 解決辦法 |
|---|---|---|---|---|
| day3 | 架構設計 | 不直接回傳 Entity，改用 DTO | Entity 若有敏感欄位（如 userId）會直接暴露給前端 | 建立獨立的 `*Response` DTO，Service 層做轉換 |
| day3 | 架構設計 | 統一錯誤格式（GlobalExceptionHandler） | 各 Controller 的錯誤格式不一，前端難以統一處理 | 建立 `GlobalExceptionHandler`，所有錯誤統一回傳標準 JSON |
| day4 | Bug | `ArgumentCaptor` 才能驗證存入資料庫的值 | 只驗證回傳值抓不到「Service 把數字改掉再存」的 bug（如把 1000 改成 0） | 用 `ArgumentCaptor` 在 `save()` 被呼叫時攔截參數，直接比對存入值 |
| day5 | Bug | IDE 無法啟動後端（Java Runtime 路徑問題） | 直接在終端機下 `mvn` 指令找不到 Java Runtime | 改用 IntelliJ 點擊綠色三角形啟動，環境最穩定 |
| day5 | Bug | REST Client 裡 UUID 解析失敗（中文字串未替換） | 測試腳本裡的「替換成實際UUID」沒有換成真實 UUID，Spring 無法解析 | 確認測試腳本的佔位符全部換成實際值 |
| day7 | Bug | 複製貼上：`categoryRepository` 誤用在 Account 查詢 | 大量複製貼上導致 Repository 注入物件用錯 | Code Review 時注意 Repository 的使用對象是否與業務對象一致 |
| day7 | 架構設計 | 瀑布流邏輯：條件判斷要從嚴格到寬鬆 | 查詢條件若順序寫反，寬鬆條件會提早攔截，嚴格條件永遠跑不到 | 判斷鏈從最嚴格（複合條件）寫到最寬鬆（單一條件） |
| day8 | Bug | 帳戶餘額消失（動態計算未涵蓋所有交易類型） | 統計餘額的 JPQL 只計算 INCOME / EXPENSE，沒有計算 TRANSFER | 補上 TRANSFER_IN / TRANSFER_OUT 的金額計算邏輯 |
| day11 | Bug | Redis 啟動衝突（本機 brew services 佔用 6379 port） | Mac 上 Homebrew 安裝的 Redis service 已佔用 6379，Docker 無法綁定 | `brew services stop redis`，讓 Docker Redis 接管 |
| day11 | 架構設計 | Redis 預設序列化產生亂碼 | 預設的位元組序列化在 TablePlus / redis-cli 中無法閱讀 | 改用 `StringRedisSerializer`（key）+ `GenericJackson2JsonRedisSerializer`（value） |
| day12 | Bug | `@CacheEvict` 在 Service 內部呼叫自己失效 | Spring AOP 代理只攔截從外部進來的呼叫，Service 自己呼叫自己會繞過代理 | 把快取清除邏輯移出，或改用 SpEL 動態 key 直接在正確的地方清 |
| day12 | 效能 | `findAll()` 會把整張表載入記憶體 | 資產數量大時直接 OOM，系統崩潰 | 改用 `PageRequest` 分頁查詢，每次只取 100 筆 |
| day12 | Bug | `ObjectMapper` Bean 污染全域 JSON 解析 | 為 Redis 開啟 DefaultTyping 的 ObjectMapper 被 Spring MVC 拿去解析 API 請求，前端登入時因缺少 `@class` 屬性而 500 | 改為私有方法 `redisObjectMapper()`，不標記 `@Bean`，只傳給 Redis Serializer 使用 |
| day12 | Bug | DTO 的 `isExpired()` 被 Jackson 誤認為欄位 | Jackson 把 `is` 開頭的方法當作 boolean getter，序列化成 `expired` 欄位存入 Redis，反序列化時 `UnrecognizedPropertyException` | 加 `@JsonIgnore`；類別加 `@JsonIgnoreProperties(ignoreUnknown = true)` |
| day12 | 架構設計 | 快取命中時仍然 `Thread.sleep`（限速邏輯放錯位置） | sleep 和更新時間戳寫在呼叫端，即使從快取拿到資料也會休眠，回傳時間也是錯的 | 把 `@Cacheable`、時間戳、sleep 全部下放到 `CoinGeckoService` / `YahooFinanceService`，快取命中直接 return，不休眠 |
| day13 | Bug | WebSocket 握手被 Spring Security 攔截（401/403） | JWT 驗證 filter 把 `/ws` 握手請求當成一般 API 請求處理 | 在 `SecurityConfig` 把握手端點 `/ws/**` 加入 `permitAll()`，STOMP 層再做身份驗證 |
| day14 | Bug | Swagger UI 路徑 403 | Spring Security 預設封鎖所有路徑，Swagger 的靜態資源和 API docs 路徑未放行 | 把 `/swagger-ui/**`、`/v3/api-docs/**` 加入 `permitAll()` 白名單 |
| day14 | Bug | Swagger 啟動 HTTP 500 / `NoSuchMethodError` | `springdoc-openapi` 版本與 Spring Boot 3.4 不相容 | 升級到相容版本（2.8.4 以上） |
| day16 | Bug | 循環依賴（Circular Dependency）導致啟動失敗 | Service A 注入 Service B，Service B 又注入 Service A | 重新梳理依賴方向，抽出共用邏輯到第三個 Service，打破循環 |
| day22 | 架構設計 | 複合索引欄位順序：user_id 放前、date 放後 | 所有查詢都同時帶 user_id 過濾，單獨加 date 索引沒有意義 | `@Index(columnList = "user_id, date")`，先縮小用戶範圍再走日期排序 |
| day22 | Bug | JWT 過期後畫面無反應（沒有導向 /login） | Token 過期後 API 回 401，但前端沒有攔截處理 | Axios response interceptor 攔截 401，自動清除 token 並 redirect 到 `/login` |
| day23 | Bug | `sockjs-client` 在 Vite 環境報錯（global 未定義） | SockJS 是 CommonJS 套件，用了 Node.js 的 `global` 變數，瀏覽器沒有 | 在 `vite.config.ts` 加 `define: { global: 'globalThis' }` polyfill |
| day23 | Bug | WebSocket 用 `ws://` URL 連線失敗 | SockJS 自己處理協議升級，不接受 `ws://`，要傳 `http://` | 改用 `http://` URL，讓 SockJS 自行升級為 WebSocket 連線 |
| day23 | 架構設計 | WebSocket 跨元件通訊：Zustand timestamp 方案 | `MainLayout` 收到 WS 推播後，如何通知深層子元件（AssetList）刷新 | Zustand store 存 `lastPriceUpdateAt` 時間戳，`AssetList` 用 `useEffect` 監聽，timestamp 改變就 reload |
| day20~21 | Bug | 前端刪除有關聯資料的類別時沒有友善提示 | 後端拋出 Foreign Key Constraint 例外，前端直接顯示原始錯誤 | `catch` 區塊捕捉後端例外，顯示中文友善提示 |
| day24 | Bug | `Input` import 被誤刪導致頁面 crash | 把 symbol 欄位改成 AutoComplete 時順手刪掉 `Input` import，但備註欄仍用 `Input.TextArea` | 把 `Input` 加回 import 清單；教訓：刪 import 前搜尋整個檔案 |
| day24 | Bug | 單元測試驗證順序與業務邏輯不符 | `assetCostPrice == null` 的驗證放在 `findById()` 之前，「資產不存在」測試反而被 costPrice 驗證攔截，拋錯訊息不對 | 把 costPrice 驗證移入各自的業務分支（加倉 / 新增），確保驗證順序符合邏輯流程 |
| day24 | Bug | Production 出現 duplicate key（JPA flush 順序問題） | `deleteByAssetType()` 是 JPQL，Hibernate 可能把 DELETE 和後續 INSERT 排進同一批次，導致 unique constraint 衝突 | 在 delete 後顯式呼叫 `flush()`，強制 DELETE 先送達資料庫再執行 INSERT |
| day24 | 架構設計 | AutoComplete 保護機制（確認 Tag） | 選取資產後使用者仍可繼續打字，form 裡的 symbol 和畫面顯示不一致，有誤填資產的風險 | 選取後把 AutoComplete 替換成確認 Tag（`CheckCircleOutlined`），點「重新選擇」才回到輸入框 |
| day24 | 架構設計 | 轉帳金額自動計算並鎖定 | 讓使用者同時填「總金額」和「成本價」，兩個欄位本質相同，容易搞混 | 改為填「數量」+「單價」，金額 = 數量 × 單價自動計算，欄位設為 `disabled` |
| day25 | Bug | 加密貨幣（USD）與台股（TWD）市值混算 | CoinGecko 回 USD、Yahoo Finance 回 TWD，存入同一個 `currentPrice` 欄位，前端加總後標示 TWD，數字完全錯誤 | `Asset` entity 加 `priceCurrency` 欄位；前端統計卡片按幣別分組顯示，不跨幣別加總 |
| day25 | Bug | 加 `currency` 欄位後 Redis 反序列化失敗 | `PriceData` 實作 `Serializable`，class 結構改變導致 Java 重新算 `serialVersionUID`，舊快取物件無法反序列化，API 全壞 | 立即：`redis-cli FLUSHALL` 清快取。根本：加明確的 `serialVersionUID = 2L`，之後加欄位不會改變 UID |
| day25 | 架構設計 | 匯率 fallback 防禦設計 | Yahoo Finance 若無法取得 `USDTWD=X`，投資組合總計 Banner 不能直接壞掉 | fallback 回傳預設值 32.0，並在 UI 上標示「預設值」，降級而非崩潰 |
| day25 | 工具 | `docker-compose down -v` 清掉本地資料庫 | `-v` 參數會一起刪 Docker volume，PostgreSQL 資料完全清空；之後啟動 Hibernate 把空 tables 重建 | 本地開發不加 `-v`；Production 用 Cloud SQL，不受 Docker volume 影響 |

---

## 從 git 歷史補充的事件

| Commit | 分類 | 標題 | 詳情 | 解決辦法 |
|---|---|---|---|---|
| `a5fcdc1` | Bug | Spring Boot 啟動失敗：`PriceAlertService` 循環依賴 | `PriceAlertService` 依賴 `PriceService`，`PriceService` 間接依賴回來，Spring 無法建立 Bean | 把即時價格補充邏輯下放到 Controller 層（呼叫端），打破 Service 層的循環 |
| `37e92e9` | 架構設計 | 轉帳記錄被統計到收支，導致帳戶餘額雙重計算 | `TRANSFER_IN` 被算成收入、`TRANSFER_OUT` 算成支出，加總後錢憑空多出來 | 統計查詢和餘額計算一律排除 `TRANSFER_*` 類型；`transferGroupId` 讓兩筆記錄配對，刪一筆自動刪另一筆 |
| `78aa860` | Bug | CoinGecko `/coins/list` 回應超過 WebClient 緩衝區上限，靜默失敗 | WebClient 預設 256KB，CoinGecko 幣種清單約 1MB，超過後直接拋例外但被吞掉，同步看起來成功實際上沒資料 | 用 `ExchangeStrategies` 設定 WebClient buffer 為 5MB |
| `b341277` | Bug | 啟動時只判斷「知名資產 DB 是否為空」，導致 CRYPTO 沒被同步 | `known_assets` 有台股資料就判定「不是空的」，跳過全部同步；CRYPTO 欄位永遠是空的 | 改為對每個 `assetType`（STOCK_TW / STOCK_TWO / CRYPTO）個別判斷是否為空再決定是否同步 |
| `2f372d2` | Bug | CoinGecko 部分幣種 symbol 超過欄位長度，`INSERT` 失敗 | 欄位定義 `VARCHAR(20)`，部分垃圾幣 symbol 長達 38 字元，批次寫入整批失敗 | 調整欄位長度：`display_code` 20→100、`symbol` 30→100、`name` 100→200 |
| `5bd6f5e` | 架構設計 | 把 CoinGecko 來源從 `/coins/list` 換成 `/coins/markets` | `/coins/list` 有 15,000+ 幣種含大量下架垃圾幣，搜尋結果品質很差 | 改用 `/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=200`，只同步市值前 200 名，搜尋結果順序帶 `market_cap_rank` |
| `5bd6f5e` | Bug | 舊資產 CoinGecko ID 存大寫，API 查詢 404 | CoinGecko API 要求 id 是小寫（如 `bitcoin`），舊資料存成 `BITCOIN` 導致查價格回 404 | `CoinGeckoService.getPrice()` 統一對輸入做 `toLowerCase()`，相容舊資料 |
| `5bd6f5e` | Bug | `LocalDateTime` 預設序列化為陣列，前端顯示 `Invalid Date` | Jackson 預設把 `LocalDateTime` 序列化成 `[2026,3,1,12,0,0]` 數字陣列，`new Date(陣列)` 解析失敗 | 在 `application.yaml` 加 `write-dates-as-timestamps: false`，強制輸出 ISO 8601 字串 |
| `7f50d7d` | Bug | `@Retryable` / `@Transactional` 在 Service 內部自呼叫完全失效 | `KnownAssetSyncService.syncAll()` 用 `this::syncTwse` 呼叫自己，繞過 Spring AOP 代理，retry 和 transaction 都沒生效 | 移除 `syncAll()`，改由 `KnownAssetSyncScheduler` 直接透過注入的 proxy 呼叫各 sync 方法 |
| `06327ca` | Bug | `Thread.sleep` 放在 `getPrice()` 內，使用者手動刷新也被卡住 | 限速 sleep 寫在 `CoinGeckoService.getPrice()` 裡，快取命中時也會休眠，使用者觸發的即時查詢同樣被阻塞 | 把 sleep 移到 `PriceService.updateAllAssetPrices()` 的批次迴圈裡，只在實際呼叫外部 API（非快取命中）後才休眠 |
| `5c7e178` | Bug | CI 啟動時 `@EventListener` 呼叫外部 API 導致測試失敗 | `@SpringBootTest` 啟動完整 Spring context，`ApplicationReadyEvent` 觸發 `KnownAssetSyncScheduler` 呼叫 TWSE / CoinGecko，CI 沒有外部網路 | 在測試設定中用 `@MockitoBean` 替換 `KnownAssetSyncScheduler`，阻止外部 API 呼叫 |
| `4e35918` | 部署 | CI 後端測試無法啟動 Spring context（沒有 PostgreSQL / Redis） | GitHub Actions 的 runner 沒有資料庫，`@SpringBootTest` 連線失敗，整個 test job 掛掉 | 在 workflow yml 加 `services:` 區塊，起 `postgres:16` 和 `redis:7` 容器 |
| `02d1782` | 部署 | ESLint v8→v9 升級導致 CI lint 失敗 | ESLint v9 改用 flat config，`--ext` flag 已移除；`eslint-plugin-react-hooks` 也需要對應的 v9 版 | 升級所有相關 eslint 套件、更新 `eslint.config.js` 格式、修掉所有 lint 錯誤（`any` 型別等） |
| `8b671ab` | 部署 | Cloud Run 滾動更新期間資料庫連線數超上限，FATAL 錯誤 | `db-f1-micro` `max_connections=25`，滾動更新時舊版和新版同時存在，預設 `HikariCP pool-size=10` × 2 個實例 = 20，加上其他連線就超過 25 | 把 `maximum-pool-size` 降為 5，`minimum-idle` 降為 2，讓多版本共存時也不超限 |
| `b5f75df` | 部署 | CI 拉 Docker 基底映像時觸發 Docker Hub 流量限制 | GitHub Actions 使用的 IP 屬於大量用戶共用，Docker Hub 對匿名用戶有拉取次數限制（每 6 小時 100 次） | 把 `Dockerfile` 的基底映像改成 `mirror.gcr.io/library/eclipse-temurin`，從 Google 的 GCR 快取拉取，不限速 |
| `a775357` | 部署 | `--set-env-vars` 多行環境變數注入失敗 | `gcloud run deploy --set-env-vars` 在 shell 多行字串時帶入空白字元，導致 env var 值解析錯誤 | 改用 Python 讀取 yaml 再寫成 `KEY=VALUE` 格式的檔案，用 `--env-vars-file` 注入 |
| `a775357` | 部署 | 第一次部署到 Cloud SQL 失敗（schema 不存在） | `ddl-auto: validate` 要求 schema 已存在才能啟動，但 Cloud SQL 是全新的空資料庫 | 第一次部署改用 `ddl-auto: update` 讓 Hibernate 自動建 schema；之後保持 `update` 避免風險 |
| `dcacf23` / `ac2a754` | 部署 | Production CORS 擋掉 Firebase Hosting 的 API 請求與 WebSocket 握手 | CORS 設定只允許 `localhost`，部署後 Firebase 的 domain 被擋；WebSocket 的 CORS 和 Spring Security 的 CORS 是**分開**設定的，漏掉一個就 403 | 在 `SecurityConfig` 的 CORS 設定和 `WebSocketConfig` 的 `setAllowedOrigins` 都加上 Firebase Hosting domain |

---

## 常用 Debug 指令備查

```bash
# 清 Redis 快取（序列化不相容時的急救措施）
docker exec -it pocketfolio-redis redis-cli FLUSHALL

# 查資料庫各 table 筆數
docker exec -it pocketfolio-db psql -U admin -d pocketfolio -c \
  "SELECT 'users' as t, COUNT(*) FROM users
   UNION ALL SELECT 'accounts', COUNT(*) FROM accounts
   UNION ALL SELECT 'transactions', COUNT(*) FROM transactions
   UNION ALL SELECT 'assets', COUNT(*) FROM assets;"

# 列出所有 tables
docker exec -it pocketfolio-db psql -U admin -d pocketfolio -c "\dt"
```
