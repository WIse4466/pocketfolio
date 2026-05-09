# day 24

## 轉帳連結資產（feature/transfer-link-asset）

### 任務說明

在交易頁建立轉帳時，若目標帳戶是投資帳戶，使用者可以：
- **加倉**：選擇既有資產，追加數量並重新計算加權平均成本
- **新增資產**：直接從轉帳動作同時建立一筆資產記錄

這樣「轉帳資金到投資帳戶」和「記錄買進資產」可以一次完成，不用分兩步操作。

---

### 後端設計

#### `TransactionRequest` 加入 7 個選填欄位

```java
private UUID assetId;         // 加倉：指定既有資產 ID
private AssetType assetType;
private String assetSymbol;
private String assetName;
private BigDecimal assetQuantity;
private BigDecimal assetCostPrice;  // 單價，amount = qty × costPrice（前端計算）
private String assetNote;
```

#### `TransactionService.linkAssetIfNeeded()` 邏輯

1. 目標帳戶不是 INVESTMENT → 直接 return（不影響現有流程）
2. 沒有帶資產欄位 → 直接 return
3. **加倉路徑**：找到資產 → 驗證所有權與帳戶歸屬 → 更新數量 + 加權平均成本
4. **新增路徑**：驗證 symbol 唯一 → 建立新的 Asset entity

加權平均成本公式：
```
新成本價 = (舊數量 × 舊成本價 + 加倉數量 × 買入單價) / (舊數量 + 加倉數量)
```

---

### 前端設計

#### 核心 UX 決策：金額欄位自動計算並鎖定

一開始設計讓使用者填「總金額」和「成本價」，後來發現兩個欄位其實是同一件事的不同呈現，容易混淆。

最終決定：使用者填「**數量**」和「**單價**」，金額 = 數量 × 單價自動計算，金額欄位 `disabled`。

#### AutoComplete 保護機制

問題：AutoComplete 是文字輸入框，使用者選完後還可以繼續打字，導致 `form` 裡的 `symbol` 和搜尋框顯示的文字不一致，有記錯資產的風險。

解法：選取後把 AutoComplete 換成一個確認 Tag，只有點「重新選擇」才能回到輸入框。

```tsx
{selectedAssetDisplay ? (
  <Tag icon={<CheckCircleOutlined />} color="success">
    {selectedAssetDisplay}
  </Tag>
  <Button type="link" onClick={clearSelectedAsset}>重新選擇</Button>
) : (
  <AutoComplete ... />
)}
```

同樣的保護機制也套用到資產管理頁（AssetList）。

#### 選取後顯示當前市價

選完資產後呼叫 `/api/prices/{symbol}?type=CRYPTO` 取得即時價格，顯示在確認 Tag 旁邊，讓使用者填單價時有參考依據。

---

### 遇到的問題

#### 問題 1：Input 未定義的 crash

把 symbol 欄位改成 AutoComplete 時，刪掉了 `Input` 的 import，但 `Input.TextArea`（備註欄）還在用它。頁面直接 crash。

解法：把 `Input` 加回 import 清單。教訓：刪 import 前先確認整個檔案的用法。

#### 問題 2：單元測試驗證順序錯誤

`linkAssetIfNeeded()` 裡把 `assetCostPrice == null` 的驗證放在 `assetRepository.findById()` 之前。結果「資產不存在」和「資產屬於別人」這兩個測試案例在驗證 costPrice 那一關就先失敗了，拋出錯誤訊息不對。

解法：把 costPrice 的驗證移到各自的 if 分支內部（加倉分支 / 新增分支），確保驗證順序與業務邏輯流程一致。

#### 問題 3：Production 出現 duplicate key 錯誤

```
ERROR: duplicate key value violates unique constraint "known_assets_symbol_key"
```

`KnownAssetSyncService` 在 `deleteByAssetType()` 之後直接 `saveAll()`，但 JPA 的 `deleteByAssetType()` 是 JPQL，flush 時機由 Hibernate 決定——Hibernate 可能把 DELETE 和 INSERT 排在同一批次，導致新舊資料的 unique constraint 衝突。

解法：在 delete 後加 `flush()`，強制 DELETE 先抵達資料庫，再執行 INSERT。

```java
knownAssetRepository.deleteByAssetType("CRYPTO");
knownAssetRepository.flush();  // 確保 DELETE 先送達
knownAssetRepository.saveAll(toSave);
```

這個 bug 在開這個 feature 的時候在 production 爆出來，開了獨立的 hotfix branch（`fix/known-asset-sync-flush`，PR #13），stash 手邊的工作 → 切去修 → PR merge → 回來繼續。

---

### 單元測試（TransactionServiceTest，8 個新測試）

| 測試案例 | 驗證重點 |
|---|---|
| `linkAsset_nonInvestmentAccount_noAssetOperation` | 非投資帳戶不觸發資產邏輯 |
| `linkAsset_withAssetId_updatesQuantityAndWeightedCost` | 加倉加權平均成本正確（10@800 + 10@900 = 850） |
| `linkAsset_withNewSymbol_createsAsset` | 新資產 symbol 自動轉大寫 |
| `linkAsset_zeroQuantity_throws` | 數量為 0 拋例外 |
| `linkAsset_assetIdNotFound_throws` | 找不到資產 ID 拋例外 |
| `linkAsset_assetOwnedByOther_throws` | 資產屬於別人拋例外 |
| `linkAsset_assetInDifferentAccount_throws` | 資產在不同帳戶拋例外 |
| `linkAsset_duplicateSymbol_throws` | 同帳戶重複 symbol 拋例外 |

---

### 面試素材 🗣️

「你怎麼確保轉帳和資產更新的一致性？」

「兩個操作都在同一個 `@Transactional` 方法裡。Spring 的事務管理確保轉帳記錄和資產更新要麼全部成功、要麼全部 rollback，不會出現轉帳成功但資產沒更新的中間狀態。」

「加權平均成本是什麼，你為什麼用這個？」

「這是投資裡計算持倉成本的標準方式。例如第一次買 10 股 @800，第二次加倉 10 股 @900，加權平均成本就是 850，而不是直接用最新的 900。這樣損益計算才能反映真實的平均持倉成本。公式：(舊數量 × 舊成本 + 新數量 × 新單價) / 總數量。」

「你遇過 JPA 的 flush 問題嗎？」

「有。在同步資產清單的 Service 裡，我做了 deleteAll 再 saveAll，Hibernate 有時會把兩者放在同一個 flush batch，DELETE 還沒到資料庫，INSERT 就先進去，觸發 unique constraint 衝突。解法是在 delete 後顯式呼叫 `flush()`，強制 Hibernate 先送 DELETE，這在 production 確實出現過，所以印象很深。」
