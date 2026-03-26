# Claude Code 使用指南

## 🚀 快速開始

### 1. 首次啟動
```bash
# 確保在專案根目錄
pwd  # 應該顯示 .../pocketfolio

# 查看專案結構
ls -la

# 閱讀專案上下文
cat docs/PROJECT_CONTEXT.md
```

### 2. 了解當前狀態

**閱讀順序：**
1. `docs/PROJECT_CONTEXT.md` - 專案概覽
2. `docs/TODO.md` - 待辦事項
3. `docs/CODING_STANDARDS.md` - 編碼規範
4. `README.md` - 啟動指南

### 3. 常用命令

**後端開發：**
```bash
cd backend
./mvnw clean install        # 編譯
./mvnw spring-boot:run      # 啟動
./mvnw test                 # 測試
```

**前端開發：**
```bash
cd frontend
npm install                 # 安裝依賴
npm run dev                 # 開發模式
npm run build               # 建構生產版本
npm run preview             # 預覽生產版本
```

**Docker：**
```bash
docker-compose up -d        # 啟動資料庫
docker-compose down         # 停止
docker-compose logs -f      # 查看日誌
```

---

## 📚 重要文件位置

### 配置文件
- 後端配置：`backend/src/main/resources/application.yaml`
- 前端配置：`frontend/.env`
- Docker 配置：`docker-compose.yaml`

### 核心代碼
- 後端入口：`backend/src/main/java/com/pocketfolio/backend/BackendApplication.java`
- 前端入口：`frontend/src/main.tsx`
- 路由配置：`frontend/src/router.tsx`

### 文檔
- API 文檔：http://localhost:8080/swagger-ui.html
- 專案文檔：`docs/`
- README：`README.md`

---

## 🎯 常見任務

### 新增一個 API

**後端步驟：**
1. 在 `entity/` 建立實體（如果需要）
2. 在 `repository/` 建立 Repository
3. 在 `dto/` 建立 Request/Response DTO
4. 在 `service/` 實作業務邏輯
5. 在 `controller/` 建立 REST 端點
6. 加入 Swagger 註解

**前端步驟：**
1. 在 `types/` 定義 TypeScript 類型
2. 在 `api/` 建立 API 服務
3. 在 `pages/` 建立或更新頁面組件

### 新增一個頁面

1. 在 `frontend/src/pages/` 建立組件
2. 在 `router.tsx` 加入路由
3. 在 `MainLayout.tsx` 加入選單項目
4. 整合 API 服務
5. 加入錯誤處理和 loading 狀態

### 除錯

**後端：**
```bash
# 查看日誌
tail -f backend/logs/application.log

# 或直接在 terminal 看 console 輸出
./mvnw spring-boot:run
```

**前端：**
- 使用瀏覽器 DevTools
- 查看 Network 面板（API 請求）
- 查看 Console（錯誤訊息）

**資料庫：**
```bash
# 連接 PostgreSQL
docker exec -it pocketfolio-postgres psql -U admin -d pocketfolio

# 查看表格
\dt

# 查詢資料
SELECT * FROM users;
```

---

## ⚠️ 重要提醒

### 不要做的事

1. **不要修改 User Entity 的 getUsername()**
   - 它必須返回 email（UserDetails 介面要求）
   - 使用 displayName 欄位

2. **不要移除資料隔離邏輯**
   - 所有查詢都必須過濾 userId
   - 使用 SecurityUtil.getCurrentUserId()

3. **不要在 Service 之間建立循環依賴**
   - 使用 Controller 層組裝資料

4. **不要在前端儲存敏感資訊**
   - 只儲存 Token
   - 密碼永不儲存

### 必須做的事

1. **所有 API 都要有 Swagger 註解**
```java
   @Operation(summary = "建立交易")
   @Tag(name = "2. 交易記錄")
```

2. **所有表單都要驗證**
```typescript
   rules={[{ required: true, message: '請輸入' }]}
```

3. **錯誤處理要完整**
```typescript
   try {
     await api.call();
   } catch (error) {
     // Axios 攔截器已處理，這裡記錄即可
     console.error('操作失敗', error);
   }
```

4. **提交前測試**
   - 後端：`./mvnw test`
   - 前端：手動測試主要功能
   - 確保沒有 console.error

---

## 🔄 開發流程

### 1. 開始新功能
```bash
# 建立功能分支
git checkout -b feature/your-feature-name

# 查看待辦事項
cat docs/TODO.md

# 閱讀相關文檔
cat docs/PROJECT_CONTEXT.md
```

### 2. 開發中

- 遵循 `docs/CODING_STANDARDS.md`
- 參考現有代碼風格
- 及時提交（小步快跑）

### 3. 完成後
```bash
# 執行測試
cd backend && ./mvnw test
cd frontend && npm run build

# 提交代碼
git add .
git commit -m "feat(scope): description"

# 更新 TODO
# 更新 DEVELOPMENT_LOG.md
```

---

## 📝 提交訊息範例
```
feat(websocket): 新增即時價格推播功能
fix(auth): 修復 Token 過期後無法登出的問題
docs(readme): 更新環境變數說明
refactor(service): 提取共用驗證邏輯
test(transaction): 新增交易建立測試
chore(deps): 升級 React 到 18.3.0
```

---

## 🆘 常見問題

### Q: 後端無法連接資料庫

A: 確認 Docker 容器運行中
```bash
docker ps | grep postgres
docker-compose up -d
```

### Q: 前端 API 請求 401

A: 檢查 Token 是否有效
- 重新登入取得新 Token
- 檢查 localStorage.getItem('token')

### Q: WebSocket 無法連接

A: 檢查 CORS 設定和防火牆
- SecurityConfig.java 的 CORS 設定
- 確認 8080 port 開放

### Q: 找不到某個類別/函數

A: 使用全專案搜尋
```bash
# 搜尋文件
grep -r "TransactionService" backend/src/

# 或使用 IDE 搜尋功能（推薦）
```

---

## 🎓 學習資源

**Spring Boot：**
- https://spring.io/guides
- https://docs.spring.io/spring-boot/docs/current/reference/html/

**React：**
- https://react.dev/
- https://ant.design/components/overview

**TypeScript：**
- https://www.typescriptlang.org/docs/

---

## 📞 需要幫助？

1. 查看 `docs/` 資料夾
2. 閱讀代碼註解
3. 參考類似功能的實作
4. 查看 Git 歷史：`git log --oneline`