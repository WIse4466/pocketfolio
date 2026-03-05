# day 9: Spring Security 與 JWT 身分驗證實戰

---
## 🌟 本日核心成就
成功為 Pocketfolio 專案建置了業界標準的 JWT (JSON Web Token) 無狀態身分驗證系統。實作了從註冊加密、登入核發 Token、到 API 門禁攔截與全域錯誤處理的完整流程。

---
## 🔑 1. JWT 與資安核心觀念
### JWT (JSON Web Token) 的組成與設定
- secret (防偽印章)：至少 256 bits 的高強度密碼，用於伺服器端簽名 (HMAC-SHA)，絕對不可外流。JWT 防偽造但不防偷看。

- expiration (有效期限)：通常設定為 24 小時（單位：毫秒 86400000），避免 Token 外流後被永久盜用。

- Claims (聲明)：印在 JWT 中間段 (Payload) 的 JSON 資料。

  - 標準聲明：sub (Subject/擁有者), iat (發行時間), exp (過期時間)。

  - 自訂聲明：可自訂放入 Role 或 UserId，但絕對不可放入密碼等敏感資訊。

### 網路傳輸安全
- 為什麼前端傳明文明碼是安全的？ 
因為現代網路有 HTTPS (TLS/SSL) 加密隧道保護傳輸過程。進入後端後，立刻交給 PasswordEncoder 攪碎成不可逆的 BCrypt 亂碼存入資料庫。前端不應該自己先做 Hash，以免引發 Pass-the-Hash 攻擊。

### Web 基礎觀念
- CSRF (跨站請求偽造)：利用瀏覽器自動帶 Cookie 的弱點進行攻擊。因為我們改用 JWT（需手動塞入 Header），所以可以用 .csrf(disable) 安全關閉防護。

- CORS (跨來源資源共用)：瀏覽器預設阻擋跨網域請求。後端必須設定白名單（Allowed Origins, Methods, Headers），並允許 OPTIONS 請求（Preflight 探路兵）通過。

- Stateless Session (無狀態)：伺服器不使用記憶體紀錄登入狀態，完全「認 JWT 手環不認人」，大幅提升系統效能與併發能力。

## 🛡️ 2. Spring Security 四大天王與實作元件
用「夜店安保系統」來理解：

1. UsernamePasswordAuthenticationToken (訪客單)：將前端傳來的 Email 與明文密碼包裝起來的容器（尚未驗證）。

2. AuthenticationManager (安保總管)：Spring Security 的核心大腦，負責比對帳號密碼，驗證成功後核發正式的 Authentication（通行金牌）。

3. PasswordEncoder (碎紙機)：使用 BCryptPasswordEncoder，將明文密碼單向加密，並負責比對前端輸入的密碼與資料庫的亂碼是否吻合。

4. Authentication (通行金牌)：驗證通過後的實體，包含使用者的完整資訊 (Principal) 與權限 (Authorities)。

### 核心 Java 類別與職責
- SecurityConfig (總司令部)：負責組合所有元件、設定 CORS、關閉 CSRF/Session，並制定每支 API 的門禁規則 (authorizeHttpRequests)。

- JwtUtil (發卡與驗證機)：純粹的工具類別。將 generateToken 與 createToken 拆開實作，符合封裝與 DRY 原則，保留未來擴充不同角色 Token 的彈性。

- CustomUserDetailsService (檔案室管理員)：實作 UserDetailsService 介面，當總管需要查人時，負責去資料庫 (UserRepository) 把實體 User 撈出來。

- JwtAuthenticationFilter (第一線保鑣)：繼承 OncePerRequestFilter。攔截請求 ➔ 擷取 Bearer  後的 Token ➔ 解碼出 Email ➔ 查閱資料庫 ➔ 驗證無誤後將身分掛上 SecurityContextHolder (全店廣播)。

## ☕ 3. 重要 Java 與 Spring 語法
- 泛型與通配符 (Collection<? extends GrantedAuthority>)：
宣告一個集合，裡面可以裝任何物件，前提是該物件必須實作了 GrantedAuthority 介面。

- 防爆箱 (Optional<User>)：
用來避免 NullPointerException (NPE)。配合 .orElseThrow() 強迫工程師處理「找不到資料」的情境。

- Jakarta Validation (@Valid, @NotBlank, @Email)：
取代舊版的 javax。在 Controller 層就將格式錯誤的 JSON 擋下，不讓髒資料進入 Service 邏輯。

- Spring IoC 容器 (@Bean)：
在 Config 中標註 @Bean，告訴 Spring 啟動時將該物件（如 PasswordEncoder）實例化並收入倉庫，供全專案透過 @RequiredArgsConstructor 依賴注入 (DI) 共用。

## 🐛 4. 錯誤處理與 Debug 技巧
### 全域例外處理 (GlobalExceptionHandler)
負責將伺服器拋出的 Exception 攔截，包裝成前端友善的 JSON 格式與正確的 HTTP Status Code：

- 401 Unauthorized (未授權/身分不明)：密碼錯誤、查無此人、Token 無效。

- 403 Forbidden (權限不足)：Token 合法，但試圖存取權限不符的資源（例如一般會員闖入 Admin 後台）。

### 客製化 Spring Security 的預設 403 行為
若未帶 Token 闖入受保護的 API，Spring 預設會丟出醜陋的 403。我們在 SecurityConfig 塞入公關講稿：

```java
.exceptionHandling(exceptions -> exceptions
.authenticationEntryPoint((request, response, authException) -> {
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 強制改為 401
response.setContentType("application/json;charset=UTF-8");
response.getWriter().write("{\"message\": \"身分驗證失敗：Token 無效或已過期\"}");
})
)
```
### Debug 偵探守則
1. 看懂 Stack Trace (報錯瀑布)：永遠從最底下往上找 Caused by:，那才是真正的「致死原因」。

2. YAML 縮排陷阱：YAML 靠縮排決定階層。jwt: 必須放在最左側根目錄，不可縮排於 spring: 之下，否則 @Value("${jwt.secret}") 會找不到值 (PlaceholderResolutionException)。

## JWT 參考連結
https://medium.com/%E4%BC%81%E9%B5%9D%E4%B9%9F%E6%87%82%E7%A8%8B%E5%BC%8F%E8%A8%AD%E8%A8%88/jwt-json-web-token-%E5%8E%9F%E7%90%86%E4%BB%8B%E7%B4%B9-74abfafad7ba
