# Day 5: 擴充核心實體 (Category) 與 API 實戰排錯

## 🏗️ 一、Java 與 JPA 基礎建設

### 1. Enum (列舉) 與資料庫儲存策略
* **概念**：`enum` 是一種特別的資料型別，用來定義「一組固定、不可變的常數」（例如：`INCOME` 收入、`EXPENSE` 支出），它能讓 IDE 在編譯階段就抓出打字錯誤，比單純用 `String` 安全得多。
* **JPA 儲存策略 (`@Enumerated`)**：
    * ❌ `EnumType.ORDINAL` (預設)：存數字索引 (0, 1...)。極度危險，若未來修改 enum 順序會導致資料庫數據污染。
    * ✅ `EnumType.STRING` (強烈推薦)：直接存字串 (如 "INCOME")。安全且在資料庫中具備極高可讀性。

### 2. Package (套件) 的兩大功用
* **實體資料夾**：必須跟硬碟上的資料夾路徑完全一致。
* **命名空間 (Namespace)**：防止類別名稱撞車（例如 `entity.Category` 和 `dto.Category` 不會互相干擾）。業界習慣用「公司網址反寫」（如 `com.pocketfolio`）作為開頭以確保全球唯一性。

---

## 🪄 二、Spring Data JPA 黑魔法：方法命名查詢



不需要寫任何 SQL 語法，只要「按照官方規定的文法幫方法取名字」，Spring 就會在背景自動翻譯並生成 SQL！

* **過濾查詢 (`findBy...`)**：
  `List<Category> findByType(CategoryType type);`
  👉 翻譯為：`SELECT * FROM categories WHERE type = ?;`
* **存在性檢查 (`existsBy...`)**：
  `boolean existsByName(String name);`
  👉 翻譯為：`SELECT COUNT(*) > 0 FROM categories WHERE name = ?;`

⚠️ **踩坑血淚史 (PropertyReferenceException)**：
這套魔法對「文法」要求極度嚴格！今天不小心寫成了 `existByName` (少了一個 s)，Spring 就會以為你要找一個叫做 `existByName` 的變數而導致程式崩潰 (`No property 'existByName' found`)。**務必遵守第三人稱單數加 s 的規則！**

---

## 🏭 三、現代 Java 的優雅寫法：Stream API

將實體 (Entity) 轉換為傳輸物件 (DTO) 的標準「流水線」寫法：



```java
public List<CategoryResponse> getAllCategories() {
    return repository.findAll()        // 1. 【進貨】從資料庫拿出所有實體
            .stream()                  // 2. 【上輸送帶】轉換成資料流
            .map(this::toResponse)     // 3. 【加工】(方法參照) 將每個實體映射/轉換為 DTO
            .collect(Collectors.toList()); // 4. 【裝箱】收集成一個新的 List 回傳
}
```
- 優勢：取代傳統冗長且易出錯的 for 迴圈，改用「宣告式」語法，讓程式碼讀起來就像在描述一個加工流程的故事。

## 🐛 四、實戰除錯與 REST Client 測試技巧
### 1. 伺服器啟動與連線被拒絕 (Connection Rejected)
- 症狀：在 VS Code 發送 API 請求，卻得到 "Connection was rejected"。

- 原因：Spring Boot 伺服器（廚房）根本沒啟動，沒有人在監聽預設的 8080 port。

- 解法：不要在終端機下指令（容易遇到找不到 Java Runtime 的路徑問題），直接在 IntelliJ 點擊 BackendApplication 旁邊的綠色三角形啟動，最穩也最方便。

### 2. .http 測試腳本的變數地雷 (500 Error)
- 症狀：使用 @categoryId 查詢單筆資料時，伺服器回傳 500 Internal Server Error。

- 原因：在 .http 檔案設定 UUID 變數時加上了雙引號（如 @categoryId = "uuid..."）。REST Client 會把雙引號一起送給後端，導致 Java 解析 UUID 失敗崩潰。

- 解法：變數直接貼上純文字即可：@categoryId = 6099d9...

### 3. 見證客訴部 (GlobalExceptionHandler) 的威力
- 當發生上述 UUID 解析失敗的嚴重錯誤時，系統並沒有噴出醜陋的 Java Stack Trace 給前端，而是回傳了我們定義好的優雅 JSON：

```JSON
{
"message": "伺服器發生錯誤，請稍後再試",
"timestamp": "2026-02-23T12:57:03.972022"
}
```