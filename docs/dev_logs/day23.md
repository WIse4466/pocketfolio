# day23

## WebSocket 前端整合

建立連線後，後端每5分鐘更新一次價格，更新完就透過/topic/price-updates 廣播出去。

| 步驟 | 工作 |
|------|------|
| 1 | 安裝套件：`@stomp/stompjs` + `sockjs-client` |
| 2 | 建立 `useWebSocket` hook -- 管理連線、訂閱、斷線 |
| 3 | 在`AssetList`頁面接收價格更新、刷新資料 |
| 4 | 顯示連線狀態（已連線/連線中/以斷線） | 

因為選擇做全網站的通知，因此需考慮跨元件溝通

## WebSocket 跨元件溝通設計

### 架構說明

**MainLayout（全域）**
- 建立 WebSocket 連線
- 訂閱 `/topic/price-updates` → 觸發 `AssetList` 重新載入
- 訂閱 `/user/queue/alerts` → 彈出 Ant Design 通知（任何頁面都看得到）

---

### 問題

**跨元件溝通問題：**  
MainLayout 收到價格更新，要怎麼通知 AssetList？

---

### 解法

需要一個「共享狀態」來做元件之間的同步。

這裡使用 **Zustand store（與 auth 同一個工具）**：
- 存一個「最後更新時間戳（timestamp）」
- `AssetList` 監聽這個 timestamp
- 當 timestamp 改變 → 自動 reload

---

### 實作步驟（6 步）

| 步驟 | 工作 | 檔案 |
|------|------|------|
| 1 | 安裝套件 | — |
| 2 | 建立 websocketStore（Zustand） | `store/websocketStore.ts` |
| 3 | 建立 useWebSocket hook | `hooks/useWebSocket.ts` |
| 4 | 修改 MainLayout：<br>• 建立連線<br>• 警報通知<br>• 顯示連線狀態 | `MainLayout.tsx` |
| 5 | 修改 AssetList：<br>• 監聽更新<br>• 自動 reload | `AssetList.tsx` |
| 6 | 測試 + commit | — |

## 面試素材
「你有做即時功能嗎？」

「有，用 WebSocket 實作即時價格更新和警報通知。技術選型上選了 STOMP over SockJS——STOMP 是訊息協議，讓我可以用『訂閱頻道』的概念收資料，比原生 WebSocket 的 onmessage 更語意清晰；SockJS 是降級方案，當瀏覽器不支援 WebSocket 時自動改用 HTTP polling，提升相容性。」

「連線架構怎麼設計的？」

「連線建在 MainLayout 層，這樣不管用戶在哪個頁面都能收到警報。用 Zustand 共享連線狀態，當後端廣播價格更新時，把時間戳存進 store，資產頁面監聽這個時間戳變化就會自動重新抓資料。這是一個解耦的設計——WebSocket 只負責通知『有更新了』，不直接操作 UI。」

「遇到什麼問題？」

「兩個坑：一是 sockjs-client 是 CommonJS 套件，用了 Node.js 的 global 變數，在 Vite 的瀏覽器環境會報錯，要在 vite.config.ts 加 global: globalThis polyfill。二是 SockJS 連線 URL 必須用 http:// 而不是 ws://，因為 SockJS 自己處理協議升級，直接傳 ws:// 會噴錯。」

## 部署討論
# GCP部署

$300 credit 90 天對這個專案完全夠用。我估算一下：

| 服務 | 用途 | 費用 |
| --- | --- | --- |
| Cloud Run | Spring Boot 後端容器 | ~$0（有大量免費額度，idle 不收費） |
| Cloud SQL (db-f1-micro) | PostgreSQL | ~$10/月 |
| Firebase Hosting | React 前端靜態檔 | 免費 |
| **Upstash** (外部) | Redis | 免費（每天 10,000 指令） |

**90 天總花費約 $25-30 美金**，$300 credit 綽綽有餘。

---

**Redis 為什麼用 Upstash 而不是 GCP Memorystore：**
GCP 的 Memorystore 最低也要 ~$35/月，貴到不合理。Upstash 是 serverless Redis，免費額度對這個專案足夠，而且 Spring Boot 接起來跟本地 Redis 一樣，只是換個 host。

# 部署架構

```
GitHub push
    │
    ▼ GitHub Actions
    ├── 前端 build → Firebase Hosting（靜態 CDN）
    └── 後端 build Docker image
            │
            ▼ push to Artifact Registry
            └── deploy to Cloud Run
                    │
                    ├── Cloud SQL (PostgreSQL)
                    └── Upstash (Redis)
```

這個架構面試說起來很完整：**容器化部署、managed database、serverless 後端、CDN 靜態前端、CI/CD 自動化**。

Apple Silicon 本地 build linux/amd64 需要 --platform flag
Docker Hub rate limit → 改用 mirror.gcr.io
--set-env-vars 多行寫法會帶入空格 → 改用 Python 寫 env 檔
ddl-auto: validate 在第一次部署沒有 schema 會炸
CORS 忘了加生產環境 domain