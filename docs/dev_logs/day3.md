# Day 3
1. TransactionResponse.java    ← 回應用的 DTO（不直接回傳 Entity）
2. GlobalExceptionHandler.java ← 統一錯誤處理
3. 補齊 Controller / Service   ← GET / PUT / DELETE

## TransactionResponse
為什麼要有 Response DTO？

直接回傳 Entity 的問題：未來 Entity 加了敏感欄位（如 userId）會直接暴露；

Entity 結構改變會影響 API 格式。DTO 讓 API 格式與資料庫結構解耦。

### Builder
如果一個物件（object）包含很多的欄位，這樣在創立時需要在constructor塞很多參數，或者寫一堆的getter和setter。

如果使用 @Builder，建立物件時，寫法會變成這樣：

```java
// 優點：像是在寫英文句子一樣流暢！
TransactionDto dto = TransactionDto.builder()
.amount(new BigDecimal("150"))
.note("慶祝專案重啟喝奶茶")
.date(LocalDate.now())
.build(); // 最後加上 build() 把物件組裝起來
```

@Builder 的三大好處：
- 不用管順序：你可以先寫 .note() 再寫 .amount()，完全沒關係。
- 可讀性極高：每個值前面都跟著欄位名稱，過了半年再回來看也知道自己在寫什麼。
- 不用處理空值：如果你今天這筆交易沒有備註 (note)，你只要不寫 .note(...) 就好，它會自動幫你設為 null。不用像傳統建構子那樣被迫傳入一個 null。

## GlobalExceptionHandler
### @RestControllerAdvice
這個標籤告訴 Spring Boot，請隨時監聽所有 @RestController 的動靜，只要有任合 Controller 丟出例外而沒有處理，都交由這裡處理。

### errorBody
道歉信格式

所有錯誤回傳格式都應該長的一模一樣，回傳一個標轉的 JSON 格式，讓前端好做事。

### @ExceptionHandler 三種客訴方案
A. 找不到資料（404 Not Found）

要求刪除不存在 ID 時會觸發

B. 客人亂填資料（400 Bad Request）

DTO 有 @NotNull, @Min(0) 等標籤，漏填欄位或是負數就會觸發

C. 未知的系統災難（500 Internal Server Error）

所有其他 Exception 放在這裡

### ResponseEntity
Spring Boot 用來包裝「完整HTTP回應」的專用盒子。

可以設定
- Status
- Headers
- Body

有一些語法糖

ResponseEntity.badRequest().body(...) ＝ 回傳 400 + 內容

ResponseEntity.ok(data) ＝ 回傳 200 + 內容

ResponseEntity.notFound().build() ＝ 回傳 404，不帶內容

### Java Stream 語法
放上輸送帶，篩選，轉換，打包

```Java
List<String> words = List.of("apple", "cat", "banana", "dog");
List<String> result = new ArrayList<>();

for (String w : words) {
if (w.length() > 3) {
result.add(w.toUpperCase());
}
}
// 結果：["APPLE", "BANANA"]
```
現代的 Stream 寫法（輸送帶）：
一行解決，像是在講人話！

```Java
List<String> words = List.of("apple", "cat", "banana", "dog");

List<String> result = words.stream()                  // 1. 倒上輸送帶
.filter(w -> w.length() > 3)                      // 2. 篩選 (只留長度 > 3 的)
.map(w -> w.toUpperCase())                        // 3. 轉換 (全部變大寫)
.toList();                                        // 4. 裝箱打包
```

### ResourceNotFoundException
其實只是繼承了 RuntimeException 換了一個名字而已。

## Service
1. orElseThrow 的霸道開箱
   
    出現位置： getTransaction 和 updateTransaction 方法中。

```Java
Transaction tx = repository.findById(id)
.orElseThrow(() -> new ResourceNotFoundException("找不到 ID 為 " + id + " 的交易"));
```
- 原理解析：repository.findById(id) 回傳的不是 Transaction，而是一個 Optional<Transaction>（一個可能裝著交易、也可能空空的盒子）。
- 作用：這行程式碼的意思是：「打開盒子，如果有資料，就把 Transaction tx 交給我；如果盒子是空的，立刻中斷程式，並把 ResourceNotFoundException 丟給外面的公關部 (GlobalExceptionHandler) 去處理」。
- 優點：你完全不需要寫 if (tx == null)，乾淨俐落。

2. .map(this::toResponse) 的輸送帶加工

   出現位置： getAllTransactions 方法中。

```Java
return repository.findAll(pageable).map(this::toResponse);
```
- 原理解析：這裡的 .map() 是 Spring 分頁物件 (Page) 內建的方法，概念跟 Java Stream 完全一樣。findAll 從資料庫撈出了比如 10 筆「原始 Entity」的資料。
- this::toResponse：這叫做「方法參照 (Method Reference)」。
它告訴這條加工輸送帶：「請把這 10 筆 Entity 一個一個拿出來，全部丟進下面那個名叫 toResponse 的 Helper 方法裡，把它們轉換成 DTO（外帶餐盒），然後重新包裝成一頁回傳」。

3. Pageable 的無痛分頁
   
    出現位置： getAllTransactions 方法的參數。

- 原理解析：我們在 Controller 裡設定了預設值（例如：第 0 頁、每頁 10 筆、按日期遞減）。Controller 把這個設定包裝成 Pageable 物件，傳給了 Service。

- 作用：Service 什麼都不用算，直接把 pageable 丟給 repository.findAll(pageable)。底層的 Hibernate 會自動幫你把這個要求翻譯成 SQL 語法（例如加上 LIMIT 10 OFFSET 0），並且自動計算出「總共有幾頁」、「總共有幾筆資料」，全部打包在 Page<TransactionResponse> 裡回傳給前端。

4. build() 的最終宣告
   
    出現位置： 最下方的 toResponse Helper 方法。

```Java
return TransactionResponse.builder()
.id(tx.getId())
.amount(tx.getAmount())
.note(tx.getNote())
.date(tx.getDate())
.build(); // 這裡！
```
- 作用：沒錯，這就是**「宣告組裝結束」**！

- 原理解析：呼叫 .builder() 就像是開啟了一張點菜單，接下來的 .id(), .amount() 都是在單子上打勾。直到你呼叫 .build() 的那一瞬間，Java 才會真正依照這張點菜單去 new 出一個完整的 TransactionResponse 物件並回傳。

## Controller
### 1. `@RequestBody` (讀取點菜單)

**出現位置**：POST 或 PUT 方法，緊貼在 DTO 前面。
```java
public ResponseEntity<TransactionResponse> create(@RequestBody CreateTransactionRequest request)
```
- 原理解析：當前端透過 POST 傳送一段 JSON 資料（例如 {"amount": 100, "note": "午餐"}）放在 HTTP 請求的 Body (主體) 裡時，@RequestBody 會自動啟動 Jackson 轉換器，把這段 JSON 反序列化（解析）成 Java 的 CreateTransactionRequest 物件。
- 白話文：客人遞交了一張寫滿明細的「點菜單」，服務生 (@RequestBody) 負責把這張單子騰寫進餐廳標準的訂單系統裡。

### 2. @Valid (檢查點菜單)

**出現位置**：通常與 @RequestBody 結伴出現。
```java
public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request)
```
- 原理解析：它會觸發 DTO 裡面的驗證規則（例如你之前寫的 @NotNull, @Positive）。如果驗證失敗，程式會立刻中斷，並拋出 MethodArgumentNotValidException 交給客訴部（GlobalExceptionHandler）處理。
- 白話文：服務生拿到點菜單後， **「先檢查」** 客人有沒有漏填桌號或寫了負數的金額。如果亂寫，服務生當場退回，不會把錯誤的菜單送進廚房。

### 3. @PathVariable (讀取網址路徑)
   
**出現位置**：GET, PUT, DELETE 方法中，用來抓取網址中的變數。

```java
@GetMapping("/{id}")
public ResponseEntity<TransactionResponse> getOne(@PathVariable UUID id)
```
- 原理解析：當前端發送請求到 /api/transactions/123e4567-e89b... 時，Spring 會對照 @GetMapping("/{id}")，發現網址最後一段是一個變數 {id}。@PathVariable 就會把那串 UUID 抓出來，塞進 Java 的變數 id 裡。
- 白話文：服務生直接從客人的「桌牌號碼」（網址路徑）來確認這是哪一桌的訂單（你要操作哪一筆特定的資料）。

### 4. @PageableDefault (分頁預設值)
   
**出現位置**：GET 列表方法，放在 Pageable 參數前。

```java
@PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable
```
- 原理解析：Pageable 會去讀取網址後面的 Query Parameters (查詢參數)，例如 ?page=1&size=20。但如果前端「什麼都沒傳」（只呼叫 /api/transactions），這時 @PageableDefault 就會介入，提供一套預設的防呆值。
- 白話文：如果客人點牛排時沒說要幾分熟，餐廳就提供**「預設值」**：切 10 塊 (size=10)，按照日期 (sort=date) 排列。

### 5. Sort.Direction.DESC (降序排列)
   
**出現位置**：設定排序方向時使用（搭配 @PageableDefault）。

- 原理解析：這是一個 Enum (列舉)。DESC 代表 Descending（降序/遞減），ASC 代表 Ascending（升序/遞增）。
- 實戰意義：記帳軟體的列表，通常都是**「最新的資料排在最上面」**。所以設定 sort = "date", direction = DESC，就等於告訴資料庫：「請用 date (日期) 欄位，由新到舊排好再給我」。

## Refactor
把 CreateTransactionRequest 更名為 TransactionRequest，如果後續有需要分開再更改。

## 測試結果
```bash
wise@MacBook-Air backend % curl -X POST http://localhost:8080/api/transactions \ 
  -H "Content-Type: application/json" \
  -d '{"amount": 1000, "note": "薪水", "date": "2026-02-01"}'
{"id":"73f9b27b-707e-4fa3-b04b-32dcd27538ce","amount":1000,"note":"薪水","date":"2026-02-01"}%                                                                  wise@MacBook-Air backend % curl "http://localhost:8080/api/transactions?page=0&size=5&sort=date,desc"
{"content":[{"id":"9fc4d9f5-bf86-430e-b856-171632c06bde","amount":150.00,"note":"慶祝專案重啟喝奶茶","date":"2026-02-06"},{"id":"73f9b27b-707e-4fa3-b04b-32dcd27538ce","amount":1000.00,"note":"薪水","date":"2026-02-01"}],"pageable":{"pageNumber":0,"pageSize":5,"sort":{"empty":false,"unsorted":false,"sorted":true},"offset":0,"unpaged":false,"paged":true},"last":true,"totalElements":2,"totalPages":1,"first":true,"size":5,"number":0,"sort":{"empty":false,"unsorted":false,"sorted":true},"numberOfElements":2,"empty":false}%                                     wise@MacBook-Air backend % curl http://localhost:8080/api/transactions/{73f9b27b-707e-4fa3-b04b-32dcd27538ce}
{"id":"73f9b27b-707e-4fa3-b04b-32dcd27538ce","amount":1000.00,"note":"薪水","date":"2026-02-01"}%                                                               wise@MacBook-Air backend % curl -X PUT http://localhost:8080/api/transactions/{73f9b27b-707e-4fa3-b04b-32dcd27538ce} \
  -H "Content-Type: application/json" \
  -d '{"amount": 500, "note": "修正"}'
{"id":"73f9b27b-707e-4fa3-b04b-32dcd27538ce","amount":500,"note":"修正","date":"2026-02-01"}%                                                                   wise@MacBook-Air backend % curl -X DELETE http://localhost:8080/api/transactions/{73f9b27b-707e-4fa3-b04b-32dcd27538ce}
wise@MacBook-Air backend % curl "http://localhost:8080/api/transactions?page=0&size=5&sort=date,desc"                 
{"content":[{"id":"9fc4d9f5-bf86-430e-b856-171632c06bde","amount":150.00,"note":"慶祝專案重啟喝奶茶","date":"2026-02-06"}],"pageable":{"pageNumber":0,"pageSize":5,"sort":{"empty":false,"unsorted":false,"sorted":true},"offset":0,"unpaged":false,"paged":true},"last":true,"totalElements":1,"totalPages":1,"first":true,"size":5,"number":0,"sort":{"empty":false,"unsorted":false,"sorted":true},"numberOfElements":1,"empty":false}%                                                      wise@MacBook-Air backend % curl -X POST http://localhost:8080/api/transactions \ 
  -H "Content-Type: application/json" \
  -d '{"note": "note"}'
{"message":"amount: 金額不能為空","timestamp":"2026-02-16T01:43:03.082059"}%    wise@MacBook-Air backend % 
```