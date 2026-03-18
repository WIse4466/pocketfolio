# Code Review 檢查清單

**目標：** 在 30 分鐘內快速掃描，發現明顯問題（非完美主義重構）

**日期：** 2026-03-18

---

## ✅ 1. 架構層級（10 分鐘）

### 循環依賴
- [x] ~~PriceService ⇄ PriceAlertService~~ **已解決 ✅**
- [ ] 其他 Service 之間沒有循環依賴

### 分層清晰
- [ ] Controller 只負責 HTTP 處理
- [ ] Service 包含業務邏輯
- [ ] Repository 只負責資料存取
- [ ] DTO 與 Entity 分離

### 配置管理
- [ ] 敏感資訊（JWT secret）有注意事項
- [ ] 環境變數使用正確
- [ ] CORS 設定適當

**發現問題：**
```
1. 
```

---

## ✅ 2. 常見反模式（10 分鐘）

### N+1 查詢問題

**檢查重點：** 在循環中呼叫 Repository
```java
// ❌ 反模式
for (Asset asset : assets) {
    Account account = accountRepository.findById(asset.getAccountId());
}

// ✅ 正確
@Query("SELECT a FROM Asset a LEFT JOIN FETCH a.account")
List<Asset> findAllWithAccount();
```

**檢查位置：**
- [ ] AssetService
- [ ] AccountService
- [ ] TransactionService
- [ ] StatisticsService

**發現問題：**
```
1. 
```

---

### 重複代碼

**檢查重點：** 相似的驗證邏輯、轉換邏輯

**常見位置：**
- [ ] Service 層的用戶驗證（`if (!entity.getUser().getId().equals(currentUserId))`）
- [ ] Entity → DTO 轉換邏輯
- [ ] 異常處理

**建議：**
- 考慮提取共用方法
- 或延到 Phase 7 重構

**發現問題：**
```
1. 所有 Service 都有重複的用戶驗證邏輯 → TODO: 考慮 AOP 或 Helper
2. 
```

---

### 空值處理

**檢查重點：** 可能的 NullPointerException
```java
// ❌ 危險
BigDecimal price = priceData.getPrice();

// ✅ 安全
BigDecimal price = priceData != null ? priceData.getPrice() : null;
```

**檢查位置：**
- [ ] PriceService
- [ ] PriceAlertService
- [ ] AssetSnapshotService

**發現問題：**
```
1. 
```

---

### 資源洩漏

**檢查重點：** 未關閉的連線、流

- [ ] WebClient 使用正確
- [ ] Redis 連線有正確關閉（Spring 自動處理 ✅）
- [ ] 定時任務沒有無限循環

**發現問題：**
```
1. 
```

---

## ✅ 3. 效能問題（5 分鐘）

### 大量資料載入

**檢查重點：** 沒有使用分頁的查詢

- [x] `updateAllAssetPrices()` 使用分頁 ✅
- [ ] `findAll()` 其他使用都適當

**發現問題：**
```
1. 
```

---

### 快取策略

**檢查重點：** 快取使用是否合理

- [x] CoinGeckoService 使用 @Cacheable ✅
- [x] YahooFinanceService 使用 @Cacheable ✅
- [x] TTL 設定為 5 分鐘 ✅

**發現問題：**
```
1. 
```

---

### 資料庫查詢

**檢查重點：** 缺少索引、複雜查詢

- [x] AssetSnapshot 有索引 ✅
- [ ] 其他常查詢的欄位有索引

**發現問題：**
```
1. Transaction.date 沒有索引 → TODO: 加入索引
2. 
```

---

## ✅ 4. 安全性問題（5 分鐘）

### 權限檢查

**檢查重點：** 所有資料都過濾 userId

- [ ] TransactionService 所有方法
- [ ] CategoryService 所有方法
- [ ] AccountService 所有方法
- [ ] AssetService 所有方法
- [ ] PriceAlertService 所有方法

**發現問題：**
```
1. 
```

---

### SQL 注入

**檢查重點：** 使用參數化查詢

- [x] 所有 @Query 使用 :param 或 ?1 ✅
- [x] 沒有字串拼接 SQL ✅

**發現問題：**
```
1. 
```

---

### 敏感資訊

**檢查重點：** 密碼、Token 不要 log

- [ ] User.password 不在 DTO 中
- [ ] JWT secret 不在 log 中
- [ ] 外部 API key 不在 log 中

**發現問題：**
```
1. 
```

---

## 📊 總結

### 🔴 嚴重問題（需立即修正）
```
無
```

### 🟡 中等問題（Phase 7 前修正）
```
1. Transaction.date 缺少索引
2. Service 層有重複的用戶驗證代碼
```

### 🟢 優化建議（可選）
```
1. 考慮使用 AOP 統一處理用戶驗證
2. 考慮使用 MapStruct 自動化 DTO 轉換
```

---

## ✅ 結論

- **可以進入 Phase 5** ✅
- **沒有阻塞性問題**
- **記錄待改進項目到 TODO.md**