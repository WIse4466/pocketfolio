# Day 4: Spring Boot 單元測試 (Unit Test) 初體驗

## 📝 核心觀念：為什麼要寫測試？
單元測試是後端工程師的「防護網」。它能確保我們在修改程式碼（例如增加新功能或重構）時，不會不小心把原本正常的邏輯改壞。測試跑得極快（毫秒級），只要亮綠燈，就代表系統核心邏輯依然健康。

---

## 🎭 一、Service 層測試：Mockito 替身魔法

Service 層（主廚）充滿了商業邏輯，我們只想測試「主廚做菜的邏輯」，不想測試「冰箱（資料庫）有沒有插電」。所以我們需要使用 **Mockito** 來製作替身。

### 1. 核心註解
* `@ExtendWith(MockitoExtension.class)`：啟動 Mockito 替身系統，不啟動完整的 Spring Boot（超快！）。
* `@Mock`：**假冰箱**（替身 Repository），不會連線真實資料庫，只會照劇本演出。
* `@InjectMocks`：**真主廚**（真實 Service），Spring 會把假冰箱塞給這個真主廚。

### 2. 測試黃金公式 (AAA)
* **Arrange (準備)**：寫劇本，告訴假冰箱該怎麼做。
  `given(repository.save(any())).willReturn(savedTx);`
* **Act (執行)**：主廚真正開始做菜。
  `TransactionResponse response = service.createTransaction(request);`
* **Assert (驗證)**：檢查主廚端出來的菜（Response）對不對。
  `assertThat(response.getAmount()).isEqualByComparingTo("1000");`

### 3. 進階技巧：ArgumentCaptor (監視器)
* **問題盲點**：如果主廚偷偷把客人點的「1000元」改成「0元」才放進假冰箱，單純驗證 Response 會抓不到（因為假冰箱不管收到什麼，都會吐出設定好的 1000 元假收據）。
* **解法**：使用 `ArgumentCaptor` 在主廚把菜放進冰箱的瞬間「定格攔截」。
```java
// 1. 攔截主廚傳給冰箱的 Entity
then(repository).should().save(transactionCaptor.capture());
// 2. 取出攔截到的證物
Transaction captured = transactionCaptor.getValue();
// 3. 檢查證物內容是不是客人點單的 1000 元
assertThat(captured.getAmount()).isEqualByComparingTo(request.getAmount());
```

### 4. 行為驗證：times(1)
用來確認主廚的「SOP」是否正確，沒有偷懶也沒手抖。

then(repository).should(times(1)).save(...)：確保存檔動作「剛好發生 1 次」。

如果是 0 次代表沒存到資料；如果是 2 次代表重複扣款（超嚴重 Bug！）。

## 📦 二、重要 Java / Spring 開發觀念
### 1. Optional (保鮮盒)
概念：為了解決找不到資料時拋出 NullPointerException (空指標例外) 的發明。

Optional 就像一個不透明的保鮮盒：

有資料：Optional.of(savedTx) (裝滿的盒子)

沒資料：Optional.empty() (空的盒子)

測試時，Repository 的 findById 必須回傳裝在 Optional 裡面的結果。

### 2. 分頁測試 (PageImpl)
概念：分頁 (Page) 包含了「資料清單」以及「總頁數、總筆數」等後設資料。

因為 Page 是 Interface，測試時不能直接 new，必須使用 Spring 提供的 PageImpl 來手工打造一台「假餐車」。

```java
Page<Transaction> fakePage = new PageImpl<>(List.of(tx1, tx2), pageable, 2);
```
## 🛡️ 三、Repository 層測試：真實沙盒演習
與 Service 測試不同，Repository 測試是真槍實彈的，它驗證的是 SQL 語法與資料庫讀寫是否正確。

### 1. 魔法註解：@DataJpaTest
只要加上這個註解，Spring 就會賦予測試三大超能力：

只啟動 JPA 層：不啟動 Web 伺服器，專注測試資料庫。

自動切換記憶體資料庫：自動使用 H2 這類超輕量資料庫，不弄髒真實的 PostgreSQL。

自帶時光倒流 (Rollback)：每個 @Test 跑完後，自動把資料庫清空回到初始狀態，確保測試之間不互相干擾。

### 2. 踩坑紀錄：Maven Reload
當我們想使用 H2 記憶體資料庫時，必須在 pom.xml 加入 <dependency>。

重點：改完 pom.xml 後，絕對要點擊 IntelliJ 右上角的「Load Maven Changes (重新整理)」，讓 Maven 下載套件，否則 Spring 會因為找不到 H2 函式庫而啟動失敗 (Exit code 255)！

```XML
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```