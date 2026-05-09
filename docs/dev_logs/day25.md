# day 25

## 加密貨幣幣別不一致（feature/asset-price-currency + feature/exchange-rate）

### 問題發現

資產管理頁的「總市值」數字是錯的。

根本原因：
- Yahoo Finance 回傳台股價格是 **TWD**
- CoinGecko 回傳加密貨幣價格是 **USD**
- 兩者都存在 `asset.currentPrice`，前端直接加總並標示 `TWD`

例如持有台積電市值 300,000 TWD + BTC 市值 100,000 USD，前端顯示「總市值 400,000 TWD」——這個數字毫無意義。

---

### 解法一：追蹤幣別（feature/asset-price-currency）

#### 後端

在 `Asset` entity 加 `priceCurrency` 欄位（`VARCHAR(3)`）：

```java
@Column(length = 3)
private String priceCurrency;  // "TWD" 或 "USD"
```

在 `PriceData` DTO 也加 `currency` 欄位，讓外部 Service 主動標記：

```java
// CoinGeckoService
return PriceData.builder().price(price).currency("USD").build();

// YahooFinanceService
return PriceData.builder().price(price).currency("TWD").build();
```

`PriceService.updateAssetPrice()` 更新價格時一併寫入 `priceCurrency`。
`AssetService.createAsset()` 建立資產時依 type 初始化（CRYPTO → USD，其餘 → TWD）。

#### 前端

- 表格的成本價、當前價格、市值欄各自顯示幣別 label
- 統計卡片按幣別分兩組顯示（台股 TWD / 加密貨幣 USD），不再混算

---

### 解法二：匯率換算顯示總計（feature/exchange-rate）

分開顯示解決了「數字不混算」的問題，但使用者還是想知道「我的投資組合總共值多少」。

#### 設計思路

不引入新的外部 API——Yahoo Finance 本身就支援匯率代號 `USDTWD=X`，我們已有 `YahooFinanceService`，直接重用。

#### 後端

新建 `ExchangeRateService`：

```java
public PriceData getUsdToTwd() {
    PriceData data = yahooFinanceService.getPrice("USDTWD=X");
    // 若無法取得，回傳預設值 32.0 並標記 source = FALLBACK
    if (data == null) return fallback();
    data.setCurrency("TWD");
    return data;
}
```

快取直接沿用 `YahooFinanceService` 的 `@Cacheable`（Redis，5 分鐘 TTL），不需要額外設定。

新增 `GET /api/exchange-rate/usd-twd` endpoint。

#### 前端

`AssetList` 頁面載入時同步拉匯率，當帳戶同時有台股和加密貨幣時，在統計卡片下方顯示「投資組合總市值（TWD）」Banner：

```
投資組合總市值（TWD）　1,234,567 TWD
匯率：1 USD = 32.54 TWD
```

計算公式：
```
totalTwd = twdMarketValue + usdMarketValue × rate
```

---

### 遇到的問題

#### 問題 1：Redis 反序列化失敗 → 使用者登入後出錯

替 `PriceData` 加了 `currency` 欄位後，Redis 裡還快取著舊的 `PriceData` 序列化物件（沒有 `currency` 欄位）。Java 的 `Serializable` 機制在 class 結構改變時會算出不同的 `serialVersionUID`，導致反序列化舊快取時拋出 `InvalidClassException`，整個 API 層壞掉。

**診斷過程**：使用者登入後出錯，直覺以為是新功能的 bug，但其實是快取問題。

**立即修復**：
```bash
docker exec -it pocketfolio-redis redis-cli FLUSHALL
```

**根本修復**：在 `PriceData` 加上明確的 `serialVersionUID`：
```java
private static final long serialVersionUID = 2L;
```

有了明確的版本號，之後再加欄位，Java 不會重新算 UID，舊快取讀到新欄位時直接用 `null` 填入，不會爆炸。

**教訓**：任何實作 `Serializable` 且被放進快取的 class，改欄位時都必須：
1. 清一次現有快取（立即解決）
2. 設定明確的 `serialVersionUID`（防止下次再發生）

#### 問題 2：本地資料庫資料被清空

清 Redis 之後，使用者發現資料庫也空了。

**診斷**：
```bash
docker exec -it pocketfolio-db psql -U admin -d pocketfolio -c \
  "SELECT 'users', COUNT(*) FROM users UNION ALL SELECT 'assets', COUNT(*) FROM assets;"
```
結果全部 0 筆，但 tables 結構存在。

**根本原因**：這跟程式改動無關。`ddl-auto: update` 只會加欄位，不會刪資料。最可能的原因是先前執行過 `docker-compose down -v`，`-v` 參數會一起刪掉 Docker volume（PostgreSQL 的資料就存在 volume 裡），之後 `docker-compose up` 時 Hibernate 把空白 tables 重建出來。

**處理方式**：本地資料重新建立（重新註冊帳號）。Production 的 Cloud SQL 不受影響。

**教訓**：本地開發做 `docker-compose down` 時，除非刻意要清資料，否則不要加 `-v`。

---

### 架構小結

```
CoinGeckoService  → PriceData { price: 95000, currency: "USD" }
YahooFinanceService → PriceData { price: 1025, currency: "TWD" }
                          ↓
                   PriceService.updateAssetPrice()
                   asset.currentPrice = 95000
                   asset.priceCurrency = "USD"   ← 新增
                          ↓
                   AssetResponse { priceCurrency: "USD" }
                          ↓ (frontend)
                   分組顯示 + 匯率換算 Banner
```

---

### 面試素材 🗣️

「你的系統同時有台股和加密貨幣，幣別怎麼處理？」

「這是我主動發現並修掉的一個 bug。原本 CoinGecko 回傳 USD 價格、Yahoo Finance 回傳 TWD 價格，兩者直接存在同一個欄位裡，前端加總後標示 TWD——數字是完全錯的。我在 Asset entity 加了 `priceCurrency` 欄位，每次外部 API 回傳價格時一併記錄幣別。前端則把台股和加密貨幣的統計分開顯示，再透過 Yahoo Finance 的匯率代號 `USDTWD=X` 取得即時匯率，換算出以 TWD 計的投資組合總市值。」

「你加了新欄位之後有沒有遇到什麼問題？」

「有，踩了 Java 序列化的坑。`PriceData` 這個 class 是存在 Redis 裡的快取物件，它實作了 `Serializable`。我加了 `currency` 欄位後，Java 因為 class 結構改變而重新算出不同的 `serialVersionUID`，導致舊的快取物件無法反序列化，API 全部壞掉。修法是先手動清快取，然後在 class 上加明確的 `serialVersionUID = 2L`，之後再加欄位就不會有這個問題了。」

「匯率怎麼取得的，有沒有考慮過第三方 API 掛掉的情況？」

「匯率用 Yahoo Finance 的 `USDTWD=X` 代號，因為我們已經有 Yahoo Finance 的 WebClient，不需要引入新的第三方服務。快取 5 分鐘，對匯率這種慢變動資料已經很夠。掛掉的部分有做 fallback：如果 API 回傳 null，就用預設值 32.0，並在 UI 上標示『預設值』讓使用者知道這不是即時數據。這是一個防禦性設計——降級，而不是直接壞掉。」
