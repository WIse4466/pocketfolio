# Day 6: 核心實體關聯 (Account & Asset) 與金融級架構設計

## 🔗 一、JPA 實體關聯映射 (Relational Mapping)

今天我們把「帳戶 (Account)」與「資產 (Asset)」用 **一對多 (One-to-Many)** 的關係串接起來了。

### 1. `@ManyToOne` 與 `@JoinColumn` (外鍵標籤)
在 `Asset` (多方) 裡面，必須標示它屬於哪個 `Account`：
* `@JoinColumn(name = "account_id")`：告訴資料庫在 `assets` 表格裡建立一個 `account_id` 欄位，用來記住「主人的 ID」。

### 2. `@OneToMany` 的連續技組合拳
在 `Account` (一方) 裡面，我們用了一個強大的設定來管理底下的資產：
```java
@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Asset> assets = new ArrayList<>();
```
- mappedBy = "account"：指定由 Asset 類別裡的 account 變數來負責管理外鍵，避免 JPA 亂建中介表。

- cascade = CascadeType.ALL (連鎖反應)：當刪除帳戶時，底下的資產也會跟著被自動全部刪除。

- orphanRemoval = true (孤兒移除)：當我們把某個資產從 assets 列表中移除時，JPA 會自動去資料庫把這筆變成「孤兒」的資料徹底刪除。
---
## 💰 二、金融系統的精確度與動態計算
### 1. 為什麼需要 scale = 8？（柴犬幣的教訓）
- 處理金融資料絕對不能用 double 或 float，必須使用 BigDecimal。

- @Column(precision = 19, scale = 8)

- 如果只保留 2 位小數，在加密貨幣的世界裡（如比特幣、柴犬幣等單價極低的幣種），會因為四捨五入變成 0，導致成本計算錯誤甚至程式當機。儲存要精確 (8位)，顯示時才四捨五入 (2位)。

### 2. @Transient (隱形斗篷)
- 用途：告訴資料庫「請當作沒看到它，不要為它建立欄位」。

- 應用場景：如 Asset 的「總市值 (Market Value)」或 Account 的「當前餘額 (Current Balance)」。這些數字是隨股價或交易變動的，不該存死在資料庫，而應該在查詢當下即時計算。
---
### 📦 三、Java 進階語法：靜態內部類別 (Static Nested Class)
在設計 DTO 時，我們常會用到這招來整理資料結構：

```java
public class AccountResponse {
// ...
public static class AssetSummary { ... }
}
```
- 為什麼要加 static？

  - ❌ 不加 static (一般內部類別)：像「人體與心臟」的寄生關係。底層會暗藏牽繩連向外部實體，容易導致 Memory Leak，且 new 的語法極度不直覺。

  - ✅ 加上 static (靜態內部類別)：像「背包與手機」的獨立關係。它是一個完全獨立的 Class，只是剛好收納在 AccountResponse 的命名空間下。可以直接獨立使用（如 .builder().build()），是 DTO 階層設計的業界標準。
---
## 🌐 四、RESTful API 核心觀念：POST vs PUT
- @PostMapping (新增 Create)

  - 概念：無中生有。去戶政事務所申請一張全新的身分證。

  - 網址：POST /api/assets (不需要給 ID，後端會自動生成)。

  - 冪等性 (Idempotency)：非冪等。連續發送 5 次會產生 5 筆不同的新資料。

- @PutMapping (更新/覆寫 Update)

  - 概念：拿著舊身分證去換新資料。

  - 網址：PUT /api/assets/{id} (必須明確告知要改哪一筆)。

  - 冪等性 (Idempotency)：冪等。連續發送 5 次，結果依然是原本那一筆資料被更新，不會產生多餘資料。