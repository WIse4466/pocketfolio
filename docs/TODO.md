# PocketFolio 待辦事項

更新時間：2026-03-27

---

## ✅ Phase 6 已全部完成

- [x] WebSocket 即時價格更新（STOMP + SockJS，全域連線）
- [x] 資產歷史快照頁面（投資組合走勢圖）
- [x] 資料庫複合索引（user_id, date）

---

## 🔴 Phase 7 優先：雲端部署

**部署目標架構：**
- 前端：Firebase Hosting（靜態 CDN）
- 後端：GCP Cloud Run（容器化 Spring Boot）
- 資料庫：GCP Cloud SQL（managed PostgreSQL）
- 快取：Upstash（serverless Redis，免費）
- CI/CD：GitHub Actions

### 1. 後端容器化
**預計時間：** 1-2 小時
- [ ] 撰寫後端 `Dockerfile`（multi-stage build）
- [ ] 本地測試 `docker build` 成功
- [ ] 設定 `application-prod.yaml`（環境變數化所有敏感設定）

### 2. GCP 環境設定
**預計時間：** 1-2 小時
- [ ] 建立 GCP 專案
- [ ] 啟用 Cloud Run、Cloud SQL、Artifact Registry API
- [ ] 建立 Cloud SQL PostgreSQL 實例
- [ ] 設定 Upstash Redis（外部服務，免費）
- [ ] 本地測試連接 Cloud SQL

### 3. 前端部署設定
**預計時間：** 30 分鐘
- [ ] 設定 `frontend/.env.production`（API URL 指向 Cloud Run）
- [ ] Firebase Hosting 初始化（`firebase init hosting`）

### 4. GitHub Actions CI/CD
**預計時間：** 1-2 小時
- [ ] 撰寫 `.github/workflows/deploy-backend.yml`
  - push main → build Docker → push Artifact Registry → deploy Cloud Run
- [ ] 撰寫 `.github/workflows/deploy-frontend.yml`
  - push main → npm build → deploy Firebase Hosting
- [ ] 設定 GitHub Secrets（GCP 金鑰、各項環境變數）

---

## 🟡 部署後持續改進

### 4. Token 過期主動偵測
**預計時間：** 30 分鐘
- [ ] App 啟動時解析 JWT 的 `exp` 欄位，若已過期則清除 auth 狀態並導向 `/login`
- [ ] 避免用戶看到已登入畫面但所有 API 操作都失敗的尷尬狀態

**背景：** 目前 axios 攔截器會在 API 回傳 401 時跳轉，但啟動時不會主動檢查，導致使用者感覺「點了沒反應」

### 5. 響應式設計優化
**預計時間：** 3-4 小時
- [ ] 手機版側邊欄優化
- [ ] 表格在手機上的顯示
- [ ] 圖表響應式調整
- [ ] 統計卡片佈局

### 5. Service 層重構
**預計時間：** 2-3 小時
- [ ] 提取共用驗證邏輯
- [ ] 考慮使用 AOP 或 BaseService
- [ ] 減少重複代碼

### 6. Entity → DTO 轉換自動化
**預計時間：** 2-3 小時
- [ ] 整合 MapStruct
- [ ] 自動生成 Mapper
- [ ] 移除手動 toResponse() 方法

### 7. 單元測試補完
**預計時間：** 4-5 小時
- [ ] CategoryServiceTest
- [ ] AccountServiceTest
- [ ] AssetServiceTest
- [ ] PriceServiceTest
- [ ] PriceAlertServiceTest
- [ ] AssetSnapshotServiceTest

### 8. 整合測試
**預計時間：** 3-4 小時
- [ ] API 層整合測試
- [ ] WebSocket 測試
- [ ] Redis 快取測試

---

## 🟢 低優先級（後續優化）

### 9. 前端優化
- [ ] 代碼分割
- [ ] 懶加載
- [ ] React.memo 優化
- [ ] 虛擬滾動（大列表）

### 10. API 限流
- [ ] Spring Cloud Gateway + Redis
- [ ] 或使用 Bucket4j

### 11. 監控與告警
- [ ] Spring Boot Actuator
- [ ] Prometheus + Grafana
- [ ] 日誌聚合（ELK）

### 12. 國際化
- [ ] i18n 設定
- [ ] 多語言支援

---

## ✅ 已完成

### Phase 1-5
- [x] 基礎架構
- [x] JWT 認證
- [x] 所有 CRUD 功能
- [x] 前端基礎頁面

### Phase 6 ✅ 全部完成
- [x] 資產管理頁面
- [x] 統計分析圖表
- [x] 價格警報頁面
- [x] Dashboard 真實資料
- [x] WebSocket 即時價格更新與警報通知
- [x] 資產歷史快照頁面
- [x] 資料庫複合索引優化

---

## 🎯 本週目標

**Week 11 (當前週):**
1. WebSocket 即時更新 ✅
2. 資產歷史快照頁面 ✅
3. 資料庫索引優化 ✅

**Week 12 (下週):**
1. 響應式優化
2. 效能調優
3. Phase 6 收尾

---

## 📝 筆記

- displayName 取代 username（User Entity）
- 循環依賴已解決（PriceService ⇄ PriceAlertService）
- 前端使用 Zustand 做狀態管理
- 圖表使用 Recharts