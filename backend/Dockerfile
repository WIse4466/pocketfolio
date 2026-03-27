# ── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 先複製 pom.xml，讓 Maven 下載依賴（利用 Docker layer cache）
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 再複製原始碼並打包（跳過測試）
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# 建立非 root 使用者（安全最佳實踐）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 只複製最終 JAR
COPY --from=builder /app/target/*.jar app.jar

# 切換至非 root 使用者
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
