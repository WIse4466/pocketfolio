# Day 15: Java 8 Lambda 語法與 RESTful API 設計原則
## 1. Java 8 的箭頭魔法：Lambda 運算式 (->)
   Lambda 運算式可以想像成一個**「沒有名字的微型函式」**，它讓我們可以用極度精簡的語法，把「動作」或「規則」當作參數傳遞。

語法拆解：alert -> shouldTrigger(alert, currentPrice)

- 左邊 (alert)： 輸入的變數（從清單中拿出來的每一個警報器物件）。

- 箭頭 (->)： 執行動作（將左邊的變數丟給右邊處理）。

- 右邊 (shouldTrigger(...))： 處理的規則與回傳結果。

透過 Lambda，我們可以將傳統冗長的 for 迴圈與 if 判斷，濃縮成一行優雅的宣告式程式碼，大幅提升可讀性。

## 2. Stream API 應用：filter vs forEach
   雖然兩者都使用了 -> 箭頭，但在資料處理流水線中扮演著完全不同的角色：

### A. filter (過濾器 / 品質檢驗員)
- 用途： 挑選符合條件的資料。

- 規則： 箭頭右邊的邏輯必須回傳 boolean (true 或 false)。

- 特性： 不會修改原始資料的狀態，單純將結果為 true 的物件挑選出來放入新的 Stream 中。

- 語法範例： 因為通常只有一行判斷邏輯，可省略大括號。

```java
alerts.stream().filter(alert -> shouldTrigger(alert, currentPrice))
```
### B. forEach (執行者 / 生產線工人)
- 用途： 對整批資料統一下達修改指令。

- 規則： 箭頭右邊的邏輯不需回傳值 (void)。

- 特性： 用來改變物件狀態、存入資料庫或印出 Log。

- 語法範例： 當需要執行多個動作時，必須使用 { } 將邏輯包覆起來。

```Java
triggeredAlerts.forEach(alert -> {
alert.setTriggered(true);
alert.setTriggeredAt(LocalDateTime.now());
alertRepository.save(alert);
});
```
## 3. RESTful API 設計：@PatchMapping vs @PutMapping
   在設計 API 時，選擇正確的 HTTP Method 是展現架構專業度的關鍵。兩者雖然都是用來「更新資料」，但語意和實作方式截然不同：

### @PutMapping (全面替換 Full Update)
- 概念： 將原本的物件整包丟棄，用前端傳來的新物件完全覆蓋。

- 代價： 前端必須把所有欄位（即使沒有修改的）都完整傳遞，否則未傳遞的欄位可能會在資料庫中被清空為 null。

### @PatchMapping (局部更新 Partial Update)
- 概念： 像打補丁一樣，只更新指定的欄位，其他欄位保持原樣不動。

- 應用場景： 非常適合用在「開關 (Toggle)」功能。例如啟用/停用警報器，前端只需傳遞 id 與布林值 active，不僅節省網路頻寬，也避免意外覆蓋其他重要設定（如目標價格）。

```Java
@PatchMapping("/{id}/toggle")
public ResponseEntity<PriceAlertResponse> toggleAlert(
@PathVariable UUID id,
@RequestParam boolean active)
```
### 💡 附錄：專案 API 設計慣例
- @PostMapping: 建立全新資源（新增警報器）。

- @GetMapping: 讀取資源（查詢警報器列表）。

- @PutMapping: 編輯資源的所有細節（修改警報器設定）。

- @PatchMapping: 更改單一狀態（切換警報器開關）。

- @DeleteMapping: 刪除資源（刪除警報器）。