# PocketFolio 編碼規範

## Java 後端規範

### 命名規範

**類別命名：**
- Entity: `Transaction`, `User`, `Account`
- Repository: `TransactionRepository`, `UserRepository`
- Service: `TransactionService`, `AuthService`
- Controller: `TransactionController`, `AuthController`
- DTO: `TransactionRequest`, `TransactionResponse`

**方法命名：**
- Repository: `findByUserId`, `existsByUserIdAndName`
- Service: `createTransaction`, `getTransaction`, `updateTransaction`
- Controller: `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`

**變數命名：**
- camelCase: `userId`, `currentPrice`, `targetPrice`
- 常數: `COLORS`, `DEFAULT_PAGE_SIZE`

### 代碼組織

**Controller 結構：**
```java
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "2. 交易記錄")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {
    
    private final TransactionService service;
    
    @PostMapping
    @Operation(summary = "建立交易")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTransaction(request));
    }
    
    // ... 其他方法
}
```

**Service 結構：**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository repository;
    // ... 其他依賴
    
    // ── Create ──────────
    public TransactionResponse createTransaction(TransactionRequest request) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        // ...
    }
    
    // ── Read ────────────
    // ── Update ──────────
    // ── Delete ──────────
    // ── Helper ──────────
}
```

### 異常處理

- 使用自定義異常：`ResourceNotFoundException`
- GlobalExceptionHandler 統一處理
- Controller 不處理異常，由 Service 拋出

### 日誌規範
```java
log.info("📸 資產快照已建立: {} (${}) - {}", symbol, price, date);
log.error("建立快照失敗: {} - {}", symbol, e.getMessage());
log.debug("查詢參數: userId={}, startDate={}", userId, startDate);
```

### 測試規範
```java
@SpringBootTest
@Transactional
class TransactionServiceTest {
    
    @Test
    void shouldCreateTransaction() {
        // Given
        // When
        // Then
    }
}
```

---

## TypeScript 前端規範

### 命名規範

**文件命名：**
- 組件: `TransactionList.tsx`, `CategoryForm.tsx`
- API: `transaction.api.ts`, `auth.api.ts`
- 類型: `transaction.types.ts`, `auth.types.ts`
- Store: `authStore.ts`, `globalStore.ts`

**變數命名：**
- camelCase: `userId`, `isLoading`, `handleSubmit`
- 組件: PascalCase: `TransactionList`, `MainLayout`
- 常數: UPPER_SNAKE_CASE: `API_BASE_URL`, `COLORS`

### 組件結構
```typescript
import { useState, useEffect } from 'react';
import { Table, Button } from 'antd';
import type { Transaction } from '@/types/transaction.types';

const TransactionList = () => {
  // ── State ──────────
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Transaction[]>([]);
  
  // ── Effects ────────
  useEffect(() => {
    loadData();
  }, []);
  
  // ── Handlers ───────
  const handleCreate = () => {
    // ...
  };
  
  // ── Render ─────────
  return (
    <div>
      {/* ... */}
    </div>
  );
};

export default TransactionList;
```

### API 調用
```typescript
// ✅ 正確
try {
  const data = await transactionApi.getTransactions();
  setTransactions(data);
} catch (error) {
  // 錯誤已在 axios 攔截器處理
  console.error('載入失敗', error);
}

// ❌ 錯誤
const data = await transactionApi.getTransactions();
setTransactions(data);
```

### 類型定義
```typescript
// ✅ 使用 interface
export interface Transaction {
  id: string;
  amount: number;
  date: string;
}

// ✅ 使用 type（聯合類型、工具類型）
export type CategoryType = 'INCOME' | 'EXPENSE';
export type Optional<T> = T | null | undefined;
```

### Ant Design 使用
```typescript
// ✅ 按需引入
import { Table, Button, Modal } from 'antd';

// ❌ 不要全局引入
import * as antd from 'antd';
```

---

## Git 規範

### Commit 訊息
```
feat(auth): 新增 JWT Token 刷新功能
fix(transaction): 修復日期篩選錯誤
docs(readme): 更新安裝說明
refactor(service): 提取共用驗證邏輯到 BaseService
test(transaction): 新增交易建立測試
chore(deps): 升級 Spring Boot 到 3.5.11
```

### 分支命名
```
feature/websocket-integration
feature/asset-history-chart
bugfix/transaction-date-filter
bugfix/auth-token-refresh
```

---

## 代碼審查檢查清單

### 後端

- [ ] 所有 API 都有 @Operation 註解
- [ ] 所有查詢都過濾 userId
- [ ] 異常處理適當
- [ ] 日誌記錄清楚
- [ ] DTO 驗證完整（@Valid）
- [ ] 沒有 N+1 查詢問題

### 前端

- [ ] 組件有適當的 loading 狀態
- [ ] 錯誤處理完整
- [ ] TypeScript 類型正確
- [ ] 沒有 any 類型（除非必要）
- [ ] 表單有驗證
- [ ] 響應式設計適當

---

## 效能最佳實踐

### 後端

1. **使用快取**
```java
   @Cacheable(value = "prices", key = "'crypto:'+#symbol")
```

2. **分頁查詢**
```java
   Pageable pageable = PageRequest.of(page, size);
```

3. **索引優化**
```java
   @Table(indexes = @Index(columnList = "user_id, date"))
```

### 前端

1. **代碼分割**
```typescript
   const LazyComponent = lazy(() => import('./Component'));
```

2. **防抖/節流**
```typescript
   const debouncedSearch = debounce(handleSearch, 300);
```

3. **Memo 優化**
```typescript
   const MemoizedComponent = memo(Component);
```

---

## 安全規範

### 後端

1. **永不在日誌中記錄敏感資訊**
   - 密碼
   - JWT Token
   - API Key

2. **SQL 注入防護**
   - 使用參數化查詢
   - 不要拼接 SQL

3. **CORS 配置**
```java
   .allowedOrigins("http://localhost:5173", "http://localhost:3000")
```

### 前端

1. **永不在前端儲存敏感資訊**
   - 只儲存 Token
   - 不儲存密碼

2. **XSS 防護**
   - Ant Design 預設防護
   - 避免使用 dangerouslySetInnerHTML

3. **Token 管理**
```typescript
   // ✅ 在 localStorage
   localStorage.setItem('token', token);
   
   // ❌ 不要在 URL 或 Cookie
```