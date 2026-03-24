# Day 22: Phase 6 - 資產、報價與警報 API 架構及路由整合

## 🌟 核心 API 架構設計與面試亮點 (API Design & Interview Highlights)

### 1. 資產管理 API (`asset.api.ts` & `asset.types.ts`)
* **DTO (資料傳輸物件) 完美分離：** 前端寫入的 `AssetRequest` 只包含基礎的「代號、成本、數量」；而後端回傳的 `Asset` 則包含了「市值、損益、報酬率」。
    * 🗣️ **面試說詞：**「我嚴格遵守了前端展示與後端邏輯分離的原則。所有關於市值的商業邏輯計算，全部交由 Spring Boot 後端處理後再封裝回傳，既保證了數據安全性（防止前端竄改），也減輕了前端的運算負擔。」

### 2. 即時報價 API (`price.api.ts`)
* **批次處理 (Batch Processing)：** 實作了 `updateMyAssetPrices` API。
    * 🗣️ **面試說詞：**「為了解決前端可能產生的 N+1 Request 問題，我設計了『批次更新』API。前端只需發送一次請求，後端就會一次性抓取所有關聯資產的最新價格並重新計算損益，大幅降低網路延遲與伺服器負載。」
* **快取管理 (Cache Management)：** 實作了 `clearCache` API，展現對第三方 API 呼叫頻率限制 (Rate Limit) 的防禦意識。

### 3. 價格警報 API (`priceAlertApi`)
* **精準的 RESTful 語意：**
    * 🗣️ **面試說詞：**「在設計 API 路由時，我嚴格遵守 RESTful 規範。全面修改警報條件時使用 `PUT`；但如果是畫面上單純切換『暫停/啟用』開關，我設計了專屬的 `PATCH` 路由 (`/toggle`)，只傳遞布林值進行局部更新，節省頻寬且語意明確。」
* **高擴充性解耦 (Decoupling)：** `AssetId` 設定為可選 (Optional)，讓系統不只支援已購買的資產警報，還能延伸作為「自選股觀察名單 (Watchlist)」使用。

---

## 🗺️ SPA 路由整合機制 (Routing & Navigation)

將開發好的 `<AssetList />` 完美串接進系統架構：
1.  **註冊路由 (`router.tsx`)：** 設定 `path: 'assets'` 對應 `<AssetList />` 元件。
2.  **綁定導覽列 (`MainLayout.tsx`)：** 在側邊欄設定 `onClick: () => navigate('/assets')`。

* 🗣️ **面試終極問答（畫面是怎麼切換的？）：**
    「當使用者點擊側邊欄的『資產管理』時，會觸發 `useNavigate` 改變網址為 `/assets`。React Router 偵測到網址改變後，**不會重新載入網頁 (No Page Reload)**，而是瞬間把 Layout 中間的 `<Outlet />` 抽換成 `<AssetList />` 元件。元件掛載後，裡面的 `useEffect` 就會立刻透過 Axios 向 Spring Boot 請求資料，完成標準的 SPA (單頁應用程式) 無縫切換體驗。」