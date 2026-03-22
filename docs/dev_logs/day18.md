# day18
## 技術棧選擇
### 核心框架
- React 18 - UI 框架
- Vite - 建構工具（比 CRA 快 10 倍）
- TypeScript - 型別安全（可選，建議用）
- React Router 6 - 路由管理
### UI框架（三選一）
#### 選項 A：Ant Design ⭐ 推薦給後端轉前端
- 完整的企業級組件庫
- 文檔豐富、上手快
- 適合管理後台
#### 選項 B：Material-UI (MUI)
- Google Material Design
- 組件豐富
- 社群活躍
#### 選項 C：Tailwind CSS + shadcn/ui
- 最現代、最靈活
- 學習曲線較陡
- 完全客製化
### 狀態管理（三選一）
#### 選項 A：Zustand ⭐ 推薦
- 超簡單（5 分鐘上手）
- 效能好
- 代碼少
#### 選項 B：Redux Toolkit
- 業界標準
- 生態系完整
- 較複雜
#### 選項 C：React Query + Context
- 專注伺服器狀態
- 自動快取
- 現代化
### HTTP客戶端
- Axios - HTTP 請求（熟悉的選擇）
- TanStack Query - 資料獲取與快取（進階）

## 前端 React 框架
### components/（基礎樂高積木）
可以重複的 UI 小零件。例如：按鈕、輸入框、提示彈窗、導覽列（Navbar）。

這些積木不知道自己在哪個頁面，只負責「長得好看」「被點擊時發出通知」。

### pages/（組裝好的樂高成品）
使用者真正看到的整個畫面。將components拼裝而成的成品。

### router.tsx
瀏覽器網址列改變時，畫面要跟著換。

這個檔案就是地圖，它規定了：「當使用者在網址列輸入 /login，請在畫面上顯示 Login.tsx 這個頁面；輸入 /transactions，就切換成顯示 TransactionList.tsx 這個頁面。」

### api/（對講機）
放向外發送 HTTP 請求（使用Axios）的程式碼。去向後端要資料的地方。

### store/（大腦記憶體）
專門用來記住「跨頁面都需要用到的重要資訊」（例如登入狀態、目前的深色/淺色主題）。

### types/
TypeScript 專屬的資料夾。裡面定義了各種資料的「形狀」。例如規定「一個交易紀錄的物件，裡面一定要有 amount (數字) 和 date (字串)」。這樣你在拼積木的時候，如果不小心把字串塞給需要數字的零件，編輯器就會立刻畫紅線警告你。

類似DTO

## Vite 是什麼
> vite 只是開發期的工具，不會跟著部署上雲端。

開發時會啟動一個 Local Server，作為測試用。

當前端寫完，準備要上雲端託管時，會下達一個指令: npm run build。
vite 會變成一個壓縮打包機，會把 React、TypeScript 程式碼，翻譯壓縮為純粹的 HTML、CSS 和 JavaScript 靜態檔案（通常放在一個叫 dist 的資料夾）。

至此 vite 就完成任務功成身退
- 會丟給 Nginx, AWS S3, , Vercel 或 Github Pages 這樣的「靜態託管服務」。
- 真正執行畫面的是使用者的瀏覽器，Chrome 或 Safari。

上雲端還是需要 Tomcat。

後端 Spring Boot 會裝在 AWS EC2 或 Zeabur。

## 技術選擇
### 🎨 關於 UI 框架（Ant Design）
- 面試官： 為什麼選 Ant Design，而不是現在很紅的 Tailwind CSS？

- 你的回答： 「因為這個專案的核心是複雜的商業邏輯（資產計算、報價抓取）。對於像 pocketfolio 這種包含大量數據報表、表單的財務系統，Ant Design 提供了開箱即用的企業級 Data Table 和表單驗證。這讓我能把開發火力集中在優化後端效能與 API 設計上，而不是花時間刻畫面的 CSS。」

### 🧠 關於狀態管理（Zustand vs Redux）
- 面試官： 前端有用到狀態管理嗎？為什麼不用業界標準的 Redux？

- 你的回答： 「我有評估過 Redux，但它的樣板代碼（Boilerplate）太多了。對於我目前的規模，我只需要在全域保存使用者的 JWT Token、登入狀態和目前的總資產餘額。Zustand 非常輕量且直覺，讓我能以最小的成本解決組件之間資料傳遞的問題，達到類似 Spring Boot 裡單例 Bean 的效果。」

### 🌐 關於 HTTP 客戶端（Axios）
- 面試官： 前端怎麼跟你的 Spring Boot 溝通的？為什麼用 Axios？

- 你的回答（這題能展現後端實力）： 「我選擇 Axios 主要是為了它的 『攔截器 (Interceptors)』 功能。因為我的後端有嚴格的 Spring Security 保護，每次打 API 都要帶 Token。透過 Axios 攔截器，我可以做到『只要發送請求，就自動在 Header 塞入 Bearer Token』，並且全局攔截 401 錯誤，當 Token 過期時自動把使用者踢回登入頁面，這跟後端的 Filter 概念非常完美地契合。」

### 設定檔案
1. package.json: 前端的 pom.xml
    - dependencies: 網頁正式上線後會用到的套件
    - devDependencies: 只有開發會用到的套件
    - scripts: 指令（e.g.:"dev": "vite" 啟動測試伺服器）
2. vite.config.ts

    用來告訴 Vite 要怎麼運作
    - resolve.alias（任意門捷徑@），用@就可以直接回到src/路徑。
    - server.proxy（解決 CORS 跨域大魔王的任意門）：
        瀏覽器有一個很嚴格的安全性規定（CORS）：跑在 5173 port 的網頁，不能隨便去跟 8080 port 拿資料，瀏覽器會直接擋下來報錯。

        - 這個設定的意思是告訴 Vite：「只要前端打出的 API 網址開頭是 /api 或 /ws，你就偷偷幫我把請求轉交給 http://localhost:8080（你的 Spring Boot）。」

        - 這樣一來，瀏覽器以為它是在跟同一個伺服器講話，就不會報錯了！這是前後端分離開發時的標準作法。
3. tsconfig.json
    - 告訴 TypeScript 該怎麼檢查
## 雜記
### Tomcat
Tomcat 是一個 web server，專門處理繁雜的網路通訊和 HTTP 協議。
- Listen Port: Tomcat 負責向 OS 申請，把 8080 port 交給他管。
- Parse HTTP: 將 HTTP 封包攔截，翻譯包裝為 Java 看得懂的物件（也可能是 HttpServletRequest）。
- Routing: 把包裝好的 Java 物件交給 Controller 處理。
- Response: 將 JSON 打包回 HTTP 格式，傳送回瀏覽器。
>以前要自己下載 Tomcat，把 Java 打包成 .war 檔丟到 Tomcat 執行。
現在的 Spring Boot 有內嵌式的 Tomcat（Embedded Tomcat），不用另外安裝。

## 未來規劃
Week 9：基礎架構（Day 1-3）
```
Day 1: 專案初始化
  ├── Vite + React + TypeScript 專案建立
  ├── 安裝依賴（Ant Design, Router, Axios, Zustand）
  ├── 專案結構規劃
  └── 環境變數設定

Day 2: 路由與布局
  ├── React Router 設定
  ├── 主布局組件（Header, Sidebar, Content）
  ├── 登入/註冊頁面路由
  └── 受保護路由（需登入才能訪問）

Day 3: API 整合與認證
  ├── Axios 封裝（攔截器、錯誤處理）
  ├── 認證 API（登入、註冊）
  ├── JWT Token 儲存與管理
  └── 自動帶 Token 的 HTTP 請求
```
Week 10：核心功能（Day 4-7）
```
Day 4: 交易記錄頁面
  ├── 交易列表（Table + 分頁）
  ├── 新增交易（Modal + Form）
  ├── 編輯交易
  └── 刪除交易（確認對話框）

Day 5: 類別與帳戶管理
  ├── 類別管理頁面
  ├── 帳戶管理頁面
  └── 表單驗證

Day 6: 狀態管理與優化
  ├── Zustand Store 設定
  ├── 全域狀態管理（用戶、Token）
  ├── 載入狀態與錯誤處理
  └── 樂觀更新

Day 7: 整合測試與優化
  ├── 端到端測試
  ├── 效能優化
  ├── 響應式設計調整
  └── 部署準備
```
## 專案結構
```
frontend/
├── public/
├── src/
│   ├── api/                  # API 請求封裝
│   │   ├── axios.ts         # Axios 實例配置
│   │   ├── auth.api.ts      # 認證 API
│   │   ├── transaction.api.ts
│   │   ├── category.api.ts
│   │   └── account.api.ts
│   ├── components/           # 通用組件
│   │   ├── Layout/
│   │   │   ├── MainLayout.tsx
│   │   │   ├── Header.tsx
│   │   │   └── Sidebar.tsx
│   │   ├── TransactionForm.tsx
│   │   └── ConfirmDialog.tsx
│   ├── pages/                # 頁面組件
│   │   ├── auth/
│   │   │   ├── Login.tsx
│   │   │   └── Register.tsx
│   │   ├── transactions/
│   │   │   └── TransactionList.tsx
│   │   ├── categories/
│   │   │   └── CategoryList.tsx
│   │   └── accounts/
│   │       └── AccountList.tsx
│   ├── store/                # Zustand 狀態管理
│   │   ├── authStore.ts
│   │   └── globalStore.ts
│   ├── types/                # TypeScript 類型定義
│   │   ├── auth.types.ts
│   │   ├── transaction.types.ts
│   │   └── api.types.ts
│   ├── utils/                # 工具函數
│   │   ├── token.ts
│   │   └── format.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── router.tsx            # 路由配置
├── .env                      # 環境變數
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### 1. TypeScript 的極致嚴格模式 (`verbatimModuleSyntax`)
* **現象：** 在 Vite 5 最新架構下，匯入 `interface` 或 `type` 時會狂報錯。
* **原因：** 打包工具為了追求極致速度，要求開發者必須明確標示哪些是「只在開發期檢查用的型別」。
* **解法：** 在 `import` 後面加上 `type` 關鍵字，大聲宣告這是虛擬說明書，打包時請直接刪除。
  ```typescript
  import type { User, AuthResponse } from '@/types/auth.types';
  import axios, { type AxiosError, type AxiosResponse } from 'axios';

### 2. Spring Security 的命名陷阱 (username vs email)
* **現象：** 登入後，前端拿到的使用者名稱變成了 Email。

* **原因：** Spring Security 的 `UserDetails` 介面強制要求實作 `getUsername()`，因為系統使用 Email 登入，所以覆寫該方法回傳了 Email。導致後續所有呼叫 `user.getUsername()` 的地方都被「挾持」，拿不到真正的顯示名稱。

* **架構重構 (Best Practice)：** 為了徹底解決命名衝突，將系統中代表「顯示名稱」的欄位全面正名為 `displayName`。包含資料庫欄位 (`users.display_name`)、實體類別 (`User.java`)、DTO 以及前端的 TypeScript 型別。

### 🛠️ 前端基礎建設實作 (Infrastructure)
#### 1. 通用工具庫 (Utils)
建立前端專屬的「瑞士刀」，全站共用。

- `src/utils/token.ts`：專門負責與瀏覽器 `localStorage` 互動，存取 JWT Token。

- `src/utils/format.ts`：利用 dayjs 格式化 ISO 時間；利用 `Intl.NumberFormat('zh-TW')` 將金錢自動加上千分位與貨幣符號。

#### 2. 網路通訊中心 (Axios Interceptors)
建立 `src/api/axios.ts` 作為全站 API 對講機：

- 請求攔截器 (Request Interceptor)： 每次發送請求前，自動從 `token.ts` 拿出 Token 並塞入 HTTP Header (`Authorization: Bearer ${token}`)。

- 回應攔截器 (Response Interceptor)： 統一處理後端拋出的例外。若遇到 `401 Unauthorized`，自動清除本地 Token 並將畫面重導向至 `/login` 登入頁。

#### 3. 全局狀態管理 (Zustand Store)
在 `src/store/authStore.ts` 建立前端的「身分證管理中心」：

- 負責記住目前的 `token`、`user` 資訊與 `isAuthenticated` 狀態。

- 搭配 Zustand 的 `persist` 中介軟體，自動將大腦狀態備份至 `localStorage`，完美抵禦使用者按 F5 重新整理造成的狀態遺失。

#### 4. 應用程式進入點與測試儀表板
- `src/main.tsx` (總開關)： 使用 `<ConfigProvider locale={zhTW}>` 將 Ant Design 全站語言設定為繁體中文，並將 React 注入至 HTML。

- `src/App.tsx` (測試儀表板)： 完美結合 UI (AntD)、狀態 (Zustand) 與 API (Axios)，實作完整的測試登入/登出流程，驗證前後端資料流是否打通。