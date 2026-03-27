# day 22
## 資料庫索引優化
### 任務說明：加資料庫索引
- 問題： Transaction table 目前沒有索引。當用戶有幾千筆交易，按日期查詢時，資料庫要掃描整張表（Full Table Scan），很慢。

- 解法： 在 @Table 加 indexes 參數，JPA 啟動時會自動在 PostgreSQL 建立對應的索引。

#### 決策：加哪些索引
##### 選項Ａ：只加單欄索引 date
    `@Index(name = "idx_transaction_date", columnList = "date")`
- 優點：簡單，加速「所有用戶的日期查詢」
- 缺點：實際查詢都會同時過濾`user_id`

##### 選項B：只加複合索引 (user_id, date)
    `@Index(name = "idx_transaction_user_date", columnList = "user_id, date")`
- 優點：完全符合「某用戶的日期範圍查詢」這個最常見場景，效益最大
- 缺點：無法加速「不帶 user_id 的 date 查詢」（但這個場景在這個系統不存在）

##### 選項C：兩個都加
- 優點：覆蓋所有情境
- 缺點：現在選項A的情境不存在，多一個索引會多寫入成本、儲存空間

#### 選B
#### 面試素材草稿 🗣️
「你的系統有做資料庫效能優化嗎？」

`「有。以交易記錄為例，所有查詢都是『某個用戶在某個日期範圍內的交易』，所以我在 transactions 表加了一個 複合索引 (user_id, date)。
設計重點是欄位順序：user_id 放前面，因為它的過濾效果最強（直接把資料縮小到只有該用戶的）；date 放後面，讓日期範圍查詢可以在已縮小的資料集上再走索引。
我沒有單獨加 date 索引，因為這個系統不存在『跨用戶查某日期』的場景，加了反而是浪費寫入效能和儲存空間。」`

## 資產歷史快照頁面
### 任務說明：資產歷史快照頁面
需要做 4 件事：

| 步驟 | 檔案 | 說明 |
|------|------|------|
| 1 | `frontend/src/api/snapshot.api.ts` | 封裝兩個 API 呼叫 |
| 2 | `frontend/src/pages/assets/AssetHistoryPage.tsx` | 主頁面（圖表） |
| 3 | `frontend/src/router.tsx` | 加路由 history |
| 4 | `frontend/src/components/Layout/MainLayout.tsx` | 加側邊欄入口 |

#### 決策：頁面要顯示什麼
後端有兩種資料：

A. 投資組合總覽（/portfolio/history）
→ 整個帳戶的總市值走勢，例如「我這個月資產從 $50 萬漲到 $55 萬」

B. 單一資產走勢（/asset/{id}/history）
→ 某一檔個股/幣的價格和損益，例如「我的 BTC 這 30 天的價格走勢」

頁面結構選項：

##### 選項 1：同一頁，兩個 Tab
上方 Tab 切換「投資組合」/ 「個別資產」，選資產後顯示對應圖表
→ 功能完整，但複雜度高

##### 選項 2：只做投資組合總覽
先做最有用的那個（總市值走勢），個別資產走勢之後再說
→ 簡單，快速完成，學習更聚焦

#### 選2
make it work first
### 剛才做了什麼

`snapshot.api.ts       → 呼叫 /snapshots/portfolio/history?days=N`

`AssetHistoryPage.tsx  → 3 張統計卡片 + Recharts 折線圖 + 7/30/90 天切換`

`router.tsx            → 路由 /history 對應新頁面`

`MainLayout.tsx        → 側邊欄新增「資產走勢」入口`

### 面試素材草稿 🗣️
「你的前端圖表是怎麼做的？」

`「用 Recharts 的 LineChart。資料來源是後端每天凌晨自動建立的歷史快照，前端透過 API 取得陣列資料後，直接丟進 <LineChart data={history}> 就能渲染。
有一個設計細節：圖表上有一條虛線『成本線』（ReferenceLine），標示總成本位置，讓用戶可以直觀看出目前市值是在成本線上還是下，比單純顯示數字更有視覺衝擊。」`

「資料怎麼來的？」

`「後端有一個 @Scheduled 排程，每天凌晨 1 點遍歷所有資產、抓當天價格、存成一筆 AssetSnapshot 記錄。前端只是讀取這些歷史快照，不做任何計算。這是一個典型的『讀寫分離』設計，寫的時候定時批次，讀的時候輕量快速。」`

## 測試發現問題
登入的時候 JWT token 已經過期，但沒有自動跳轉到 /login。
導致顯示在首頁，結果點所有側邊欄都沒用，因為 API 失效。
加入 TODO。