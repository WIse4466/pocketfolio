# DevOps／釋出 極簡模板（React + Spring Boot + PostgreSQL）

參考決策：ADR-002（部署策略）。本文件聚焦「怎麼做」的操作手冊與範例檔案。

- 環境：Dev（本地 Docker Compose）/ Prod（雲端）
- CI/CD：push main → 自動建置與部署
- 監控：基本健康檢查 / 錯誤通知（Email/LINE webhook）
- 回滾：保留上一版映像與 DB 備份（每日快照）

---

## GitHub Actions（極簡示意）

```yaml
name: ci
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - run: ./gradlew test build
```

---

## 0) 你要做什麼（Checklist）

- [ ] 在 GitHub 建立 Secrets：`GHCR_USERNAME`、`GHCR_TOKEN`(=PAT 或 GITHUB_TOKEN)、`SSH_HOST`、`SSH_USER`、`SSH_KEY`、`LINE_NOTIFY_TOKEN`
- [ ] 伺服器安裝 Docker / Docker Compose（或用 Render/Fly 也可）
- [ ] 把本頁檔案放進專案對應路徑
- [ ] 依 `.env.prod` 寫入 DB/密鑰，在伺服器執行：`docker compose -f infra/docker-compose.prod.yml up -d`

---

## 1) 專案結構建議

```
.
├─ backend/                # Spring Boot
│  ├─ Dockerfile
│  └─ ...
├─ frontend/               # React (Vite/CRA 皆可)
│  ├─ Dockerfile
│  └─ ...
├─ infra/
│  ├─ docker-compose.dev.yml
│  ├─ docker-compose.prod.yml
│  ├─ scripts/
│  │  ├─ monitor.sh
│  │  └─ backup_db.sh
│  └─ crontab.sample
├─ .env.sample
└─ .github/
   └─ workflows/
      ├─ ci.yml
      └─ deploy.yml
```

---

## 2) Dockerfiles

### 2.1 backend/Dockerfile（Spring Boot, Gradle）

```dockerfile
# syntax=docker/dockerfile:1
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /workspace
COPY build.gradle settings.gradle gradlew gradle/ ./
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health | grep '\"status\":\"UP\"' || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

> 若使用 Maven，改為 `mvn -DskipTests package` 與對應的 base image。

### 2.2 frontend/Dockerfile（React 靜態站）

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
# Vite=dist；CRA=build（請視專案調整）
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK CMD wget -qO- http://localhost/ >/dev/null || exit 1
```

---

## 3) Docker Compose（dev / prod）

### 3.1 infra/docker-compose.dev.yml（本地開發）

```yaml
services:
  db:
    image: postgres:16-alpine
    container_name: sb_db
    environment:
      POSTGRES_DB: superbudget
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app123
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d superbudget"]
      interval: 10s
      timeout: 3s
      retries: 10

  adminer:
    image: adminer:latest
    depends_on:
      - db
    ports:
      - "8081:8080"

volumes:
  db_data:
```

> 開發時後端設定 `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/superbudget`；前端設定 `VITE_API_BASE=http://localhost:8080`。

### 3.2 infra/docker-compose.prod.yml（雲端部署）

```yaml
services:
  db:
    image: postgres:16-alpine
    container_name: sb_db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 3s
      retries: 10

  api:
    image: ghcr.io/${GHCR_NAMESPACE}/superbudget-api:${IMAGE_TAG}
    container_name: sb_api
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health | grep '\"status\":\"UP\"' || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 5

  web:
    image: ghcr.io/${GHCR_NAMESPACE}/superbudget-web:${IMAGE_TAG}
    container_name: sb_web
    restart: unless-stopped
    depends_on:
      - api
    ports:
      - "80:80"

volumes:
  db_data:
```

> 伺服器同目錄放 `.env.prod`（下節）。更新部署：
> `IMAGE_TAG=<新tag> docker compose --env-file .env.prod -f infra/docker-compose.prod.yml pull && docker compose --env-file .env.prod -f infra/docker-compose.prod.yml up -d`

---

## 4) `.env.sample`（請複製成 `.env.prod` 放到伺服器）

```
# GHCR 命名空間（例如：username 或 org/repo）
GHCR_NAMESPACE=your-gh-namespace
IMAGE_TAG=latest

# Postgres
POSTGRES_DB=superbudget
POSTGRES_USER=app
POSTGRES_PASSWORD=change_me

# 監控通知
LINE_NOTIFY_TOKEN=xxx
HEALTH_URL=https://your.domain.com/actuator/health
```

---

## 5) GitHub Actions

### 5.1 .github/workflows/ci.yml（測試/建置）

```yaml
name: ci
on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
      - name: Build & Test (Gradle)
        run: ./gradlew --no-daemon test build

  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run build
```

### 5.2 .github/workflows/deploy.yml（推 GHCR 並遠端更新）

```yaml
name: deploy
on:
  push:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_API: ghcr.io/${{ github.repository }}-api
  IMAGE_WEB: ghcr.io/${{ github.repository }}-web

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build & Push API
        uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: true
          tags: |
            ${{ env.IMAGE_API }}:${{ github.sha }}
            ${{ env.IMAGE_API }}:latest

      - name: Build & Push WEB
        uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: |
            ${{ env.IMAGE_WEB }}:${{ github.sha }}
            ${{ env.IMAGE_WEB }}:latest

  remote-rollout:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: SSH rollout on server
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /opt/superbudget
            echo IMAGE_TAG=${GITHUB_SHA} > .env.tag
            export $(cat .env.tag | xargs)
            docker compose --env-file .env.prod -f infra/docker-compose.prod.yml pull
            docker compose --env-file .env.prod -f infra/docker-compose.prod.yml up -d
            docker system prune -f
```

> 伺服器 `/opt/superbudget` 需是你的專案目錄，且已有 `infra/docker-compose.prod.yml` 與 `.env.prod`。部署以 `IMAGE_TAG=${GITHUB_SHA}` 滾動更新，可隨時回滾到上個 SHA。

---

## 6) 監控與回滾

### 6.1 健康檢查端點（Spring Boot）

在 `build.gradle` 加入：

```groovy
dependencies { implementation 'org.springframework.boot:spring-boot-starter-actuator' }
```

`application-prod.yml`：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

> 部署後 `GET /actuator/health` 會回傳 `{ "status": "UP" }`。

### 6.2 infra/scripts/monitor.sh（LINE 通知）

```bash
#!/usr/bin/env bash
set -euo pipefail
URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"
RES=$(curl -fsS "$URL" || true)
if ! echo "$RES" | grep -q '"status":"UP"'; then
  MSG="[superbudget] Healthcheck failed at $(date '+%F %T'): $RES"
  curl -s -X POST \
    -H "Authorization: Bearer ${LINE_NOTIFY_TOKEN}" \
    -F "message=${MSG}" https://notify-api.line.me/api/notify > /dev/null || true
fi
```

> 伺服器可用 cron 每 5 分鐘跑一次。

### 6.3 infra/scripts/backup_db.sh（每日快照＋保留 7 天）

```bash
#!/usr/bin/env bash
set -euo pipefail
BACKUP_DIR=${BACKUP_DIR:-/opt/superbudget/backups}
mkdir -p "$BACKUP_DIR"
DATE=$(date +%F_%H%M)
FILE="$BACKUP_DIR/db_${DATE}.sql.gz"
docker exec -t sb_db pg_dump -U ${POSTGRES_USER:-app} ${POSTGRES_DB:-superbudget} | gzip > "$FILE"
# 清理 7 天前備份
find "$BACKUP_DIR" -name 'db_*.sql.gz' -mtime +7 -delete
```

### 6.4 infra/crontab.sample

```
# 每 5 分鐘健康檢查
*/5 * * * * LINE_NOTIFY_TOKEN=xxxx HEALTH_URL=https://your.domain/actuator/health bash /opt/superbudget/infra/scripts/monitor.sh
# 每日 03:00 備份 DB
0 3 * * * POSTGRES_USER=app POSTGRES_DB=superbudget bash /opt/superbudget/infra/scripts/backup_db.sh
```

---

## 7) 本地快速開始（Dev）

```bash
# 起 DB + Adminer
docker compose -f infra/docker-compose.dev.yml up -d
# 後端（本機）
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/superbudget \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=app123 \
./gradlew bootRun
# 前端（本機）
cd frontend && npm run dev
```

## 8) 雲端更新 / 回滾（Prod）

```bash
# 更新到特定 SHA（或 latest）
IMAGE_TAG=<GIT_SHA> docker compose --env-file .env.prod -f infra/docker-compose.prod.yml pull
IMAGE_TAG=<GIT_SHA> docker compose --env-file .env.prod -f infra/docker-compose.prod.yml up -d
# 回滾：把 IMAGE_TAG 換成舊的 SHA 重跑上面兩行
```

---

### 備註

- 不必等整個 DevOps 都完成才開始開發。建議優先完成：本地 DB + `/actuator/health` + 簡單 CI（test build），其餘逐步補齊。
- 本指南為操作層；決策脈絡與替代方案請見 ADR-002。

