# Day 19: Phase 5 - React Router 路由系統與核心 UI 頁面建置

## 🌟 核心觀念與踩坑紀錄 (Gotchas & Learnings)

### 1. 前後端欄位更名的連鎖效應 (Zustand Store 同步)
* **情境：** 後端將 `username` 改為 `displayName` 後，前端登入成功卻無法顯示名字。
* **原因：** 雖然 API 回傳了正確的資料，且 TypeScript 的型別（`interface`）已更新，但負責搬運資料的 Zustand Store (`login` 動作) 仍在解構舊的 `username` 變數。
* **解法：** 在 `authStore.ts` 中，將解構與存入的變數正確改為 `displayName`。
* **Debug 心法：** 遇到畫面資料缺失，第一步先按 `F12` 查看 **Network (網路)** 面板確認後端回傳的 JSON，第二步檢查 Frontend Store 是否正確將資料存入大腦。

### 2. React Router 的「電視機與螢幕」哲學 (`<Outlet />`)
* **觀念：** 在後台管理系統中，通常外層的「側邊欄」與「上方標題」是固定不動的。
* **實作：** 透過 React Router 的 `<Outlet />` 元件，我們可以在 `MainLayout` 中挖一個「螢幕洞口」。當網址切換時（例如從 `/` 換到 `/transactions`），只有 `<Outlet />` 裡面的內容會瞬間抽換，外殼保持不變，達成極致流暢的使用者體驗 (SPA)。

---

## 🛠️ 核心功能實作與架構 (Infrastructure & UI)

### 1. 建立門禁森嚴的導覽地圖 (`src/router.tsx`)
利用 `createBrowserRouter` 建立全站路由，並實作兩個重要的路由守衛 (Route Guards)：
* **`<PrivateRoute>` (受保護路由)：** 檢查 Zustand 大腦，未登入者強制踢回 `/login`。
* **`<PublicRoute>` (公開路由)：** 防呆機制，已登入者若嘗試訪問登入頁，強制導向控制台 `/`。
* **黑洞防護網 (`*`)：** 捕捉所有不存在的網址，導向 404 頁面。

### 2. 實作系統骨架 (`src/components/Layout/MainLayout.tsx`)
* 使用 Ant Design 的 `<Layout>`, `<Sider>`, `<Header>`, `<Content>` 搭建經典的後台版型。
* 整合 `useNavigate` 實作左側選單的點擊跳轉。
* 整合 `useAuthStore` 在右上角顯示使用者名稱 (`displayName`) 並實作登出功能。

### 3. 實作認證頁面 (`src/pages/auth/Login.tsx` & `Register.tsx`)
* 使用 Ant Design `<Form>` 實作具備自動驗證 (`rules`) 的表單。
* **技術亮點 (連動驗證)：** 在註冊頁面的「確認密碼」欄位，使用 `dependencies={['password']}` 搭配客製化 `validator`，達成即時比對兩次密碼是否一致的功能。
* **UX 優化：** 註冊成功後直接呼叫 `login()` 寫入狀態，並 `Maps('/')`，達成一氣呵成的「註冊即登入」體驗。

### 4. 實作控制台與 404 頁面 (`src/pages/Dashboard.tsx` & `NotFound.tsx`)
* **Dashboard：** 運用 Ant Design 的 `<Row>`, `<Col>` 網格系統，達成完美響應式 (RWD) 排版（手機 1 欄、平板 2 欄、電腦 4 欄）。並利用 `<Statistic>` 建立專業的財務數據卡片（目前為靜態佔位符 Placeholder，待 Phase 6 串接真實 API）。
* **NotFound：** 運用 CSS Flexbox (`display: flex`, `align-items: center`, `justify-content: center`) 搭配 AntD 的 `<Result status="404">`，優雅處理迷路的使用者。