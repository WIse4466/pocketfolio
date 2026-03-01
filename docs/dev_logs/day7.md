# Day 7: 交易系統核心串接、進階查詢與防呆設計

## 🔄 一、RESTful API 核心精神：PUT 的全量更新與解除綁定
在更新交易 (Transaction) 時，處理關聯實體 (Category, Account) 必須考慮「解除綁定 (Unlink)」的情境。
* **邏輯實作**：如果前端傳來 `categoryId = null`，後端必須明確執行 `tx.setCategory(null)`。
* **意義**：告訴資料庫清空這個外鍵 (Foreign Key)。若不寫 `else` 處理 null，使用者將永遠無法把已分類的交易改回「未分類」。

## 🛡️ 二、防禦 NullPointerException (NPE) 的金科玉律
在封裝 DTO (Builder) 時，處理可能為空的值有兩種完全不同的情況：

1. **「傳遞空值」是安全的**：
   `builder.note(tx.getNote())` 👉 即使 Note 是 null，Builder 只是把 null 裝進去，不會出錯。
2. **「向空值要東西」會引發核彈級災難**：
   `builder.categoryId(tx.getCategory().getId())` 👉 這是**連續的點 (`.`)**。如果 `getCategory()` 是 null，再去呼叫 `.getId()` 就會觸發 NPE 當機。
3. **解決方案**：必須先用 `if (tx.getCategory() != null)` 檢查「錢包存在」，才去拿「錢包裡的身分證」。
4. **扁平化 DTO (Flattened DTO)**：將巢狀的 Entity 拆解成單層的屬性（如 `categoryId`, `categoryName`），能大幅降低前端讀取資料時出錯的機率。

## 🪄 三、Spring Data JPA：黑魔法與 JPQL 統計
Repository 不只是存取資料，更是系統的報表中心。

1. **命名黑魔法 (Derived Queries)**：
   如 `findByAccountIdAndDateBetween`，Spring 會自動翻譯成對應的 SQL。
2. **分頁查詢 (`Pageable` & `Page<T>`)**：
   取代 `List<T>`，防止資料量過大撐爆記憶體，並自帶總頁數、總筆數等元資料 (Metadata)。
3. **JPQL 自定義統計 (`@Query`)**：
   ```java
   @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE ...")
   ```
關鍵防呆 COALESCE：當查詢結果沒有半筆資料時，資料庫的 SUM 會回傳 NULL。使用 COALESCE(..., 0) 能確保 Java 拿到安全的數字 0，再次防禦 NPE。

## 🎛️ 四、打造萬能查詢 API (Swiss Army Knife API)
一支 GET API 滿足前端所有篩選需求（分頁、排序、分類、帳戶、日期區間）。

1. **自動化分頁預設值**：使用 @PageableDefault 預先設定好 size 和 sort 規則。

2. **彈性參數**：使用 @RequestParam(required = false) 接收各種過濾組合。

3. **瀑布流邏輯 (Waterfall Logic)**：判斷條件必須 **「從最嚴格（複合條件）寫到最寬鬆（單一條件）」**，否則會發生條件被提早攔截的嚴重 Bug。

## ⚙️ 五、函數式編程：為什麼要用 .map()？
當資料庫回傳 Page<Transaction> 時，不能直接丟給只收 Transaction 的 toResponse 方法。

- 原理：.map(this::toResponse) 就像一條自動化加工輸送帶。它會幫你「開箱」，把裡面的實體逐一轉換成 DTO，然後再「重新裝箱」。

- 架構意義：不僅讓程式碼（宣告式編程）變得極度簡潔，更重要的是它保留了 Page 物件的分頁元資料 (Metadata)，如果手動用 for 迴圈轉換成 List，這些珍貴的分頁資訊就會全部遺失。

## 🐛 六、實體關聯查詢 vs 純值過濾
- 查 Entity (Account/Category)：必須先用 existsById 檢查存不存在，不存在應回傳 404，因為條件本身無效。

- 查 Value (Date)：日期只是屬性，不需要檢查「日期存不存在」。如果該區間沒資料，Spring 會優雅地回傳一個 content: [] 的空 Page (Empty Page)，而不是 null，HTTP 狀態碼為正常的 200 OK。

- 自我糾錯：在複製貼上程式碼時，務必小心。今天抓到了一隻在檢查 Account 存在與否時，誤用 categoryRepository 的複製貼上 Bug！