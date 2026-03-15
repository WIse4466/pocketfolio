# Day 14: Swagger UI (OpenAPI) 整合與踩坑全紀錄
## 1. 核心概念：為什麼需要 Swagger UI？
   在前後端分離的架構中，Swagger UI 可以自動將 Spring Boot 後端的 API 轉換成「可互動、視覺化」的 API 說明書網頁。前端工程師（或未來的自己）可以直接在網頁上查看 API 規格、所需參數，並直接輸入 Token 進行點擊測試，大幅降低前後端溝通與記憶的成本。

## 2. 基礎設定與 JWT 整合
### A. 全域設定檔 (OpenApiConfig.java)
建立 @Configuration 來定義 API 文件的封面資訊（標題、版本等），最重要的是加入 JWT Bearer Token 的全域安全設定，讓 Swagger 網頁右上角產生「Authorize」鎖頭按鈕。

### B. Controller 註解標籤
使用 SpringDoc 提供的註解，讓冷冰冰的程式碼變成麻瓜也看得懂的文件：

- @Tag(name = "分類名稱", description = "...")：用於 Controller 類別上，將 API 進行資料夾分類。

- @Operation(summary = "API標題", description = "...")：用於單一 API 方法上，描述該 API 的具體功能。

- @SecurityRequirement(name = "bearerAuth")：用於需要登入驗證的 Controller 或 API 上，標示該端點需要攜帶 Token 才能呼叫。

## 3. 踩坑與除錯實戰紀錄
   在導入過程中，我們遇到了三個經典的錯誤，並一一破解：

### 🐛 坑點一：網頁打不開，顯示 {"message": "身份驗證失敗..."}
- 原因： Swagger 的前端網頁資源（HTML/CSS/JS）被專案的 Spring Security (JWT 攔截器) 視為未授權的請求而擋下。

- 解法： 在 SecurityConfig.java 中，將 Swagger 相關路徑加入白名單 (permitAll())。

```java
.requestMatchers("/v3/api-docs/**").permitAll()
.requestMatchers("/swagger-ui/**").permitAll()
.requestMatchers("/swagger-ui.html").permitAll()
```
### 🐛 坑點二：畫面出來了，但顯示 Failed to load remote configuration
- 原因： 我們在 application.yml 中自訂了 API 規格檔的路徑為 /api-docs，但 SecurityConfig 白名單只放行了預設的 /v3/api-docs，導致前端網頁去後端拉取 JSON 規格時再次被擋。

- 解法： 將自訂的路徑一併加入 SecurityConfig 白名單。

```java
.requestMatchers("/api-docs/**").permitAll()
```
### 🐛 坑點三：發生 HTTP 500 錯誤與 NoSuchMethodError
- 報錯訊息： java.lang.NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'

- 原因（破壞性更新）： 專案使用了較新的 Spring Boot 3.4.x 版本，官方在此版本底層刪除了一個舊有的建構子。而舊版（如 2.5.x, 2.6.x）的 springdoc-openapi 套件在掃描全域例外處理 (@ControllerAdvice) 時，仍試圖呼叫該已被刪除的方法，導致伺服器直接崩潰。

- 最終解法： 將 springdoc-openapi-starter-webmvc-ui 的版本升級至相容 Spring Boot 3.4 的最新版（2.8.4 或以上）。

```XML
<dependency>
<groupId>org.springdoc</groupId>
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<version>2.8.4</version>
</dependency>
```
## 4. 最終成果
   完成上述設定與除錯後，啟動專案並前往 http://localhost:8080/swagger-ui.html，即可看到完整的 PocketFolio API 文件。點擊右上角「Authorize」輸入 JWT Token 後，即可直接在瀏覽器上順暢測試所有受保護的 API（例如手動觸發價格更新）。