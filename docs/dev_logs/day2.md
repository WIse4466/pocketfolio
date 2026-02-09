# Day 2
## Repository
- Repository = **資料存取層**
- 專門負責「跟資料庫拿資料 / 存資料」
- 在 Spring Data JPA 中，**人不用自己寫 SQL**

---

### 核心寫法
```java
public interface TransactionRepository 
        extends JpaRepository<Transaction, UUID> {
}
```
代表意思
Transaction：要操作的 Entity（哪一張表）

UUID：這張表的主鍵型別

只要這樣寫，Spring 就會 自動幫你產生 CRUD 功能

立刻就能用的方法
```java
transactionRepository.save(transaction);
transactionRepository.findById(uuid);
transactionRepository.findAll();
transactionRepository.deleteById(uuid);
transactionRepository.existsById(uuid);
transactionRepository.count();
```
👉 不用寫實作、不用寫 SQL

### 為什麼 Repository 是 interface？
Repository 不是人來實作

實作類是 Spring Data JPA 在執行時動態產生的

人只負責「定義需求」，Spring 負責「怎麼做」

結構關係：
```go
Repository (interface)
        ↑
Spring Data JPA 產生的實作類（人看不到）
        ↑
Service 使用它（注入）
```
### Repository 跟 Service 的關係
- Service 只依賴介面

- Service 不關心實作是誰

- 好處：

  - 解耦

  - 好測試

  - 好維護

  - 未來可換資料來源

### 重要注意事項 ⚠️
- ❌ 不要自己 new Repository

- ❌ 不要在 Service 寫資料庫邏輯

- ✅ Repository 本身就是 Spring 管理的 Bean

- ✅ Repository 的實作類 不會出現在專案資料夾

  - 只存在於 程式執行時的記憶體
  
## DTO
- 用來接收前端傳來的資料
- JSON -> Jackson -> new DTO
- 通常由 Jackson 自動 new 並填值

### @Data 會自動產生
- Getter
- Setter
- toString()
- equals()
- hashCode()
- RequiredArgsConstructor

👉 等同於：
`@Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor`

---

### 什麼時候用 @Data
✅ **DTO / Request / Response**
- 只承載資料
- 沒有商業邏輯
- 不在乎 equals / hashCode 行為
- Entity 不建議使用 @Data

```java
@Data
public class CreateTransactionRequest { ... }
```

## Service
- 處理邏輯(把 DTO 轉成 Entity)
- @Service: 告訴 Spring，這是一個 Service Bean，讓這一個類別可以被注入到 Controller
- @RequireArgsConstructor: Lombok 會產生只有 final 欄位的建構子，只要寫下列程式碼。
```java
private final TransactionRepository repository;
```
Lombok 會自動產生
```java
public TransactionService(TransactionRepository repository) {
    this.repository = repository;
}
```
這就是 Constructor Injection，有以下好處
- 不會有 null
- 好測試
- 不需要 @Autowired

final = 「只能設定一次，之後不能換」

### 建立 Entity
```java
Transaction tx = new Transaction();
```
前端送來的是 DTO，不會等於 Entity，避免讓前端控制資料庫結構。

### 存進資料庫
```java
return repository.save(tx);
```
這行會做的事：
- INSERT（如果是新資料）
- UPDATE（已有id）
- merge detached entity
- 讓 entity 進入 persistent context
- 回傳「已經被 JPA 管理的 Entity」


## Controller
- 開放 API 接口
- @RestController = @Controller + @ResponseBody
  - 回傳值會 自動轉成 JSON
  - 專門用來寫 REST API
- @RequestMapping("/api/transactions")
  - 這個 Controller 底下所有 API 的共同前綴
  - 所以完整路徑是：/api/transactions
- @RequireArgsConstructor
  - Lombok 會完成以下事項
    - 對 final 欄位產生建構子
    - Ｓpring 會用這個建構子做依賴注入
- @PostMapping
  - 處理 HTTP POST
  - 這支 API 是：POST/api/transactions
- @RequestBody
  - 把前端送來的 JSON，自動轉成 CreateTransactionRequest

| 類型         | 用途        | 產生方式         |
| ---------- | --------- | ------------ |
| Controller | 接 request | Spring 注入    |
| Service    | 商業邏輯      | Spring 注入    |
| Repository | 存取 DB     | Spring 注入    |
| Entity     | 一筆資料      | `new`        |
| DTO        | 傳資料       | `new` / JSON |

Bean = 由 Spring 容器建立並管理生命週期的物件
```bash
HTTP Request
↓
Controller
↓
Service
↓
Repository
↓
DB
```

### 測試結果
```bash
curl -X POST http://localhost:8080/api/transactions \
-H "Content-Type: application/json" \
-d '{"amount": 150, "note": "慶祝專案重啟喝奶茶"}'
```
回傳
```bash
{"id":"9fc4d9f5-bf86-430e-b856-171632c06bde","amount":150,"note":"慶祝專案重啟喝奶茶","date":"2026-02-06"}%  
```