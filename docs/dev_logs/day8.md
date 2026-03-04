# Day 8: 統計儀表板、動態餘額計算與跨服務整合

## 🧠 一、Java 8 Stream API：會計師級別的資料處理

在 `StatisticsService` 中，我們運用了高階的 Stream API，將原始的交易明細瞬間轉化為圓餅圖與統計報表所需的數據。

* **二分法分類 (`Collectors.partitioningBy`)**：
  一行程式碼將交易切成「收入」與「支出」兩個陣列，完美取代繁瑣的 `if-else`。
* **分組加總 (`Collectors.groupingBy` + `mapping` + `reducing`)**：
  依照「分類名稱」把交易分群，並自動加總同分類的花費，是製作圓餅圖最核心的演算法。
* **精確度與防呆 (`ArithmeticException`)**：
  在計算百分比時，必須先檢查分母（總金額）是否為 0：`total.compareTo(BigDecimal.ZERO) == 0`。否則當該月無花費時，會直接引發除以零的核彈級當機。

## 🏦 二、動態餘額計算與跨部門調用

`AccountService` 不再只回傳死板的初始金額，而是真正與 `Transaction` 連動，成為活的記帳本。

* **依賴注入 (Dependency Injection)**：
  在 `AccountService` 注入 `TransactionRepository`，實踐跨模組獲取資料。
* **正負號轉換流水線 (`.negate()`)**：
  使用 `BigDecimal.negate()` 將支出金額轉為負數，收入保持正數，最後透過 `.reduce` 一次性安全加總。
* **計算公式**：
  👉 `當前餘額 = 初始金額 + 總收入 - 總支出`。

## 🏢 三、架構設計：Aggregator Pattern 與 Thin Controller

`StatisticsController` 扮演了「總機小姐」與「大廳服務生」的角色，完美體現了系統設計的原則。

* **跨服務聚合 (Aggregator)**：
  首頁儀表板同時需要「月度收支」與「帳戶總覽」，因此我們開了一個專屬的 `/api/statistics` 路由，在 Controller 內部同時呼叫 `StatisticsService` 與 `AccountService` 來滿足前端的聚合需求。
* **職責分離 (Thin Controller, Fat Service)**：
  Controller 內完全沒有任何 `if-else` 或數學計算，只負責「接收參數」與「回傳 HTTP 200 狀態碼」，所有複雜的商業邏輯全部封裝在 Service 內。

## 🐛 四、實戰 Debug 紀錄：消失的餘額變動

**情境**：打 API 測試「動態帳戶餘額」時，明明資料庫有交易明細，但算出來的 `change` 和 `currentBalance` 卻絲毫未變。
**發現**：
* 一開始懷疑是 Spring Data JPA 在 `Pageable.unpaged()` 下默默回傳空陣列的隱藏陷阱。
* **真正兇手**：這是一個超級經典的開發者日常！所有的計算邏輯 (`transactionSum`) 都寫對了，但最後函數的 `return` 寫法忘記更新，依然回傳著舊的 `account.getInitialBalance()`，導致辛苦算完的結果直接被丟棄。
  **教訓**：當邏輯完美卻得不到預期結果時，先去檢查那最後一行的 `return` 是否真的把心血結晶給送出去了！