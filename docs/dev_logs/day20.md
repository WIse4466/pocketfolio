# Day 20: Phase 5 - 類別與帳戶管理頁面、API 連線實作

## 🌟 核心頁面實作與面試亮點 (Interview Highlights)

### 1. 類別管理頁面 (`CategoryList.tsx`)
這支檔案延續了標準的 CRUD 資料流，具備以下技術亮點：
* **即時狀態過濾 (Real-time Filtering)：** 利用 `Radio.Group` 綁定 `filterType` 狀態，並透過 `useEffect` 監聽。當使用者切換「收入/支出」時，自動觸發 API 重新撈取資料，不需額外的搜尋按鈕。
* **共用彈出視窗 (Shared Modal)：** 「新增」與「編輯」共用同一個表單元件，透過判斷 `editingCategory` 狀態決定執行 `POST` 或 `PUT` 請求，保持程式碼整潔 (DRY 原則)。
* **前端處理外鍵約束錯誤 (Foreign Key Constraint)：** 在刪除 API 的 `catch` 區塊中，捕捉後端可能因關聯資料（已有交易紀錄使用該類別）而拋出的例外，並給予使用者友善的中文錯誤提示，展現全端架構思維。

### 2. 帳戶管理頁面 (`AccountList.tsx`)
* **前端即時運算 (Frontend Aggregation)：** 使用 JavaScript 陣列高階方法 `reduce` (加總總資產) 與 `filter` (計算特定帳戶數量)，在不額外消耗後端資源的情況下，即時算出統計卡片上的數據。
* **字典/設定檔模式 (Dictionary / Config Pattern)：** 宣告 `accountTypeConfig` 物件來集中管理不同帳戶類型的標籤、顏色與 Icon。避免在 `render` 中寫入大量 `switch/case`，提升程式碼可維護性與擴充性。
* **極致 UX 格式化 (Formatter & Parser)：** 在金額輸入框 (`InputNumber`) 中使用 `formatter` 自動補上 `$` 與千分位逗號，並用 `parser` 在送出表單前還原為純數字，確保使用者體驗與資料庫格式完美接軌。

---

## 🎨 全域樣式優化 (`index.css`)
徹底替換 Vite 預設樣式，打造企業級後台視覺：
* 設定底色 `#f0f2f5` 以凸顯白色資料卡片與表格的層次感。
* 覆寫 Ant Design 預設樣式，為 `.ant-table-wrapper`, `.ant-modal-header`, `.ant-card` 加上統一的 `8px` 圓角與柔和陰影。
* 加上 `@media` 響應式設計，確保手機螢幕 (`max-width: 768px`) 下表格字體與按鈕能適當縮小。

---

## 🔌 API 通訊層建立 (`src/api/*.api.ts`)
建立前端與 Spring Boot 後端的橋樑，統一使用配置好攔截器的 `axiosInstance` 處理非同步請求：
* `category.api.ts`：處理類別的 CRUD，支援 `type` 查詢參數。
* `account.api.ts`：處理帳戶的 CRUD。
* `transaction.api.ts`：處理交易紀錄的 CRUD，支援多條件篩選參數 (`categoryId`, `accountId`, `startDate`, `endDate`)。