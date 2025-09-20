# Stage 1: 使用包含 Gradle 的映像檔來建構應用程式
FROM gradle:8.8.0-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

# Stage 2: 建立最終的、更小的執行環境映像檔
FROM openjdk:21-jdk-slim
WORKDIR /app
EXPOSE 8080

# 從建構階段複製已編譯好的 .jar 檔案
COPY --from=build /app/build/libs/pocketfolio-0.0.1-SNAPSHOT.jar .

# 執行應用程式
CMD ["java", "-jar", "pocketfolio-0.0.1-SNAPSHOT.jar"]