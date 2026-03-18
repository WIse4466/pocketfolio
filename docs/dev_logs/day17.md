# Day 17: Phase 5 啟航 - React 前端專案初始化與架構設計

## 🌟 核心觀念：前端的「開餐廳」哲學
在全端架構中，如果 Spring Boot 後端是「廚房」，那 React 前端就是「餐廳外場與精美菜單」。今天我們完成了店面的租借與基礎裝潢。

## 🛠️ 技術選型與面試亮點 (Trade-offs)
身為後端為主的開發者，前端技術的選擇以「快速產出、企業級規範、型別安全」為最高指導原則：

* **UI 框架：Ant Design (取代 Tailwind)**
    * **原因：** 專案包含大量財務報表與表單。Ant Design 提供開箱即用的 Data Table 與表單驗證，能大幅節省刻畫面的時間，將精力集中在商業邏輯與 API 串接。
* **狀態管理：Zustand (取代 Redux)**
    * **原因：** Redux 樣板代碼過多。系統只需全域管理 JWT Token 與登入狀態，Zustand 極度輕量且直覺，效果類似 Spring 裡的單例 Bean。
* **HTTP 客戶端：Axios (取代原生 Fetch)**
    * **原因：** 需要運用「攔截器 (Interceptors)」。能做到全局自動在 HTTP Header 塞入 Bearer Token，並集中處理 401 (Unauthorized) 錯誤，與後端的 Spring Security Filter 完美契合。
* **建構工具：Vite (取代 Webpack)**
    * **原因：** 利用 Native ES Modules 達到毫秒級啟動與熱更新 (HMR)，大幅提升開發體驗。

---

## 🚀 實作紀錄：專案初始化 (Step-by-Step)

### 1. 建立專案與安裝依賴
```bash
# 使用 Vite 建立 React + TypeScript 專案
npm create vite@latest frontend -- --template react-ts

cd frontend

# 安裝核心套件 (UI, 路由, 請求, 狀態管理, 日期處理, 圖標)
npm install antd react-router-dom axios zustand dayjs @ant-design/icons
npm install -D @types/node
```

### 2. 環境變數設定 (.env)
建立 .env 檔案，設定 API 與 WebSocket 通道：

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

### 3. 三大核心設定檔解析
- package.json (進貨清單與 SOP)

    - 記錄 dependencies (正式上線套件) 與 devDependencies (開發期輔助工具)。

    - 定義 npm run dev (啟動開發伺服器) 與 npm run build (壓縮打包上線) 等指令。

- vite.config.ts (外場營運手冊)

    - Alias (@)： 設定 @ 直接指向 src/ 目錄，解決 import 路徑過長 (../../../) 的問題。

    - Proxy (跨域任意門)： 將 /api 代理到 http://localhost:8080，完美避開前後端分離開發時最討厭的 CORS (跨域資源共用) 錯誤。

- tsconfig.json (嚴格督導檢查表)

    - 設定 "strict": true，確保 TypeScript 嚴格執行型別檢查，減少 Null 錯誤。

    - 同步設定 paths 讓編譯器看得懂 @ 捷徑。

### 4. 專案目錄結構規劃 (/src)
```Plaintext
src/
 ├── api/        # Axios 實例與 API 請求封裝
 ├── components/ # 共用 UI 組件 (Layout, Form, Dialog)
 ├── pages/      # 路由頁面 (Login, Transactions, etc.)
 ├── store/      # Zustand 全域狀態 (Auth, Global)
 ├── types/      # TypeScript 型別定義 (DTOs)
 ├── utils/      # 共用工具函數 (Format, Token)
 └── App.tsx     # 應用程式進入點
 ```

### 5. 測試啟動
執行 npm run dev，確認 Vite 伺服器成功運行於 http://localhost:5173/。
---
💡 總結： Phase 5 Day 1 目標達成。基礎設施建置完畢，下一步將進入「路由配置」與「TypeScript 型別定義」。