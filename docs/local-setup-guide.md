# 本地開發設定指南 (Local Development Setup Guide)

本文件包含在本地設定與執行此專案時，可能遇到的常見問題與其解決方案。

## 資料庫連接埠衝突 (Port 5432 Conflict)

本專案使用 Docker Compose 啟動 PostgreSQL 資料庫，並預設將容器的 `5432` 埠對應到您本地電腦的 `5432` 埠。若您電腦上已安裝並執行了其他的 PostgreSQL 服務，`docker-compose up` 指令將會因為連接埠已被佔用而失敗。

**解決方案：**

`docker-compose.yml` 檔案已預先修改，將對應關係改為 `5433:5432`。
```yaml
services:
  db:
    ports:
      - "5433:5432" # Host:Container
```
這代表 Docker 中的資料庫將會透過您本地的 `5433` 埠對外開放，藉此避免衝突。此修改不影響後端服務的連線。

## 後端原始碼同步 (Backend Live Reload)

您可能會注意到 `docker-compose.yml` 中的 `backend` 服務移除了 `volumes` 掛載。

- **原因**: 後端是 Java 編譯式語言。如果在 `docker-compose.yml` 中將本地的原始碼資料夾 (`./backend`) 掛載到容器的 `/app`，會覆蓋掉 Docker 建置映像檔時所編譯好的 `.jar` 檔案，導致容器啟動失敗。
- **工作流程**: 當您修改後端 Java 程式碼後，您需要執行 `docker-compose up -d --build` 來重新建置後端映像檔，以讓變更生效。這是使用 Docker 開發編譯式語言的標準作法。

## 前端開發環境

當您初次設定專案時，您可能會在 `frontend/src/main.tsx` 等檔案中看到語法錯誤的提示。

- **原因**: 這是因為您本地尚未安裝前端所需的依賴套件 (`node_modules`)。編輯器的 TypeScript 服務找不到 React 的型別定義，因此會將正確的語法誤判為錯誤。
- **解決方案**: 在 `frontend` 資料夾下執行 `npm install` 來安裝本地依賴。這不會影響 Docker 的建置，純粹是為了改善您本地編輯器的開發體驗。
