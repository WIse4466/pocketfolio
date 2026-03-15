# Day 13: WebSocket 即時通訊實作與架構解析
## 1. 核心觀念：WebSocket 是什麼？
   WebSocket 是一種網路通訊協定，旨在解決傳統 HTTP 無法由伺服器主動推播資料的痛點。

- 傳統 HTTP（寄信模式）： 瀏覽器發送請求，伺服器回傳資料後即斷開連線。若要獲取即時資訊，只能依賴低效的「輪詢 (Polling)」。

- WebSocket（打電話模式）： 透過一次 HTTP 握手後升級為全雙工 (Full-Duplex) 連線。連線保持暢通，伺服器與前端可以隨時且雙向地互相傳遞資料。

## 2. 系統架構定位：WebSocket vs. MQ vs. Kafka
   在處理即時資料流的系統架構中，各種工具扮演著不同的角色，彼此是合作關係而非競爭：

- Kafka (後端 ↔ 後端)： 像是「水庫」或「飛機黑盒子」。負責吞吐與持久化海量的原始串流資料（如交易所每秒萬筆的報價）。

- MQ / RabbitMQ (後端 ↔ 後端)： 像是「餐廳訂單機」。負責微服務間的任務解耦與排隊（例如耗時的發送 Email 任務），任務處理完即刪除。

- WebSocket (前端 ↔ 後端)： 像是「水龍頭」。負責把後端處理好的最終結果，零延遲地推播到用戶端的畫面上。

## 3. 程式碼解析：資料傳輸物件 (DTO)
   為了確保推播到前端的資料格式一致，後端定義了專屬的 DTO 來封裝訊息。

### A. 業務資料：PriceUpdateMessage
負責傳遞核心的報價變動資訊。

- 型別選擇： 牽涉到金融資產價格，全面採用 BigDecimal 來取代 float/double，避免二進位浮點數運算帶來的精準度遺失。

- 工廠方法： 透過 fromUpdate 靜態方法，自動計算 changeAmount (漲跌金額) 與 changePercent (漲跌幅)，減輕業務層的計算負擔，並處理了除以零的安全防護。

### B. 非業務資料：SystemMessage
負責傳遞系統層級的廣播（如 INFO, WARNING, ERROR）。

- 應用場景： 外部 API 異常警告、系統維護通知等。

- 必要性： 屬於體驗優化的加分項目，前端可依據 messageType 決定是要更新報價圖表，還是彈出系統通知 Toast。

## 4. 程式碼解析：中央派報室 WebSocketService
   透過 Spring Boot 提供的 SimpMessagingTemplate 實作訊息派送邏輯，將底層網路通訊細節封裝起來。

- 廣播模式 (Broadcast / Topic)：

  - 目的地慣例：/topic/...

  - 用途：發送給所有已訂閱該頻道的用戶。例如全站廣播 BTC 價格更新 (/topic/price-updates)。

- 單點推送模式 (Send To User / Queue)：

  - 目的地慣例：/queue/...

  - 用途：發送給特定連線的單一用戶。Spring 會自動轉換路徑（如 /user/{userId}/queue/...）。適用於個人資產結算或專屬警報通知。

## 5. 觸發與整合流程
   在 PriceService 的定時任務中完成資料庫更新後，即可觸發推播：

1. 呼叫 PriceUpdateMessage.fromUpdate() 打包最新價格。

2. 呼叫 webSocketService.broadcastPriceUpdate() 將包裹送出。

3. 達成「關注點分離」，業務邏輯與通訊邏輯互不干擾。

## 6. 測試與除錯紀錄
   在驗證 WebSocket 連線時，遇到了連線被拒（HTTP 401/403）與握手失敗的問題。

**關鍵除錯發現**
1. 協定差異： Spring Boot 使用的是 STOMP 協定，測試時不可使用 Raw WebSocket。Postman 需開啟專屬的 WebSocket (STOMP) 視窗。

2. 安全攔截 (Spring Security / JWT)：

   - 問題： 專案實作了 JWT 身分驗證。當未攜帶 Token 的測試端點嘗試進行 WebSocket 握手 (/ws) 時，直接被 Security Filter 擋下。

   - 解法： 需在 SecurityConfig.java 中將握手端點放行 (.requestMatchers("/ws/**").permitAll())，允許建立基礎連線，待進入 STOMP 協定層後再進行身分核實。

**當前進度狀態**
- 後端狀態： 🟢 運作正常。Log 顯示 PriceUpdateScheduler 成功抓取價格，且 WebSocketService 成功將資料廣播至頻道中。

- 前端狀態： ⏳ 待實作。後續將於 React 端引入 @stomp/stompjs 套件，處理連線、攜帶 Token 以及訂閱頻道的邏輯。