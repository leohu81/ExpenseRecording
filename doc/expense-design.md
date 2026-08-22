***

# 記帳 Android App ＋ Agnes 2.0 Flash 收據解析系統設計文件

## 0. 設計目標與前提

- Android 記帳 app 透過「分享截圖」接收消費紀錄。
- app 將截圖排隊送後端，後端使用 **Agnes 2.0 Flash** Vision LLM 將圖片轉為結構化 JSON。
- 每張截圖可以含多筆交易，每筆交易都拆成獨立消費記錄，並關聯回原始截圖。
- 消費方式（`method`）表示「一般刷卡 vs 透過哪個電子支付刷卡」，實際扣款的卡片記錄在 `account`（信用卡名稱）＋ `card_last4`。
- 幣別 `currency` 需記錄，支援 TWD / 外幣。
- LLM 呼叫被限制為 **每 30 秒最多一次**，並加上錯誤重試與 JSON schema 版本控管。
- 截圖：
    - Android 端只存於 **app 專用目錄**，不註冊到 MediaStore，因此不會進系統相簿。
    - 後端為了給 Agnes 使用，會暫存一份圖片於本機檔案系統，透過免費 CDN 提供短命 public image URL。
- public image URL 解法：使用 **Cloudflare Free + 自架 HTTP 靜態檔伺服器**，圖片實際存放在你的本機伺服器上，Cloudflare 僅做 DNS/CDN proxy。[^1][^2]

***

## 1. 系統組成與資料流

### 1.1 組成元件

1. **Android 記帳 App**
    - Share 接收 Activity（接 `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 的 image）。
    - 本機資料庫（Room）管理 `SourceImage` 與 `PaymentRecord`。
    - 背景工作（WorkManager / Foreground Service）負責：
        - 圖片壓縮（長邊 1080）。
        - 將圖片與 metadata 上傳後端。
        - 接收解析結果，更新本機 DB。
    - 消費記錄列表＋編輯／核准 UI。
2. **後端服務（跑在你現有 leohu.ddns.net 上）**
    - API 伺服器（例如 FastAPI / Node.js）：
        - `/api/receipt/parse`：接收圖片與電子支付帳戶清單，呼叫 Agnes 解析，回傳 JSON。
    - 靜態檔伺服器（Nginx / Caddy）：
        - 提供 `https://img.leohu.ddns.net/receipts/{uuid}.jpg` 這類 URL 給 Agnes。
        - 圖片實體存於本機，例如 `/var/www/receipts/uuid.jpg`。
3. **Cloudflare Free（建議）**
    - 管理 `img.leohu.ddns.net` DNS。
    - 將 DNS CNAME 指向你本機 HTTP 伺服器的 DDNS（例如 `leohu.ddns.net`）。
    - 啟用 proxy（橘色雲）與基本 caching 作為 CDN。
    - Cloudflare Free 適合托管一般網站與圖片靜態檔，單檔小於 512MB，流量不大時基本免費。[^2][^1]
4. **Agnes 2.0 Flash API**
    - 支援 Vision（image_url）＋文本 prompt，輸出結構化 JSON。
    - 後端以單一 worker 節流成「每 30 秒最多一次呼叫」。[^3]

***

## 2. 資料模型設計（Android 端）

### 2.1 `SourceImage`（原始截圖）

```kotlin
data class SourceImage(
    val id: String,              // UUID
    val localPath: String,       // app 專用目錄中的檔案路徑，例如 /data/data/.../files/receipts/uuid.jpg
    val createdAt: Long,         // 建立時間 (timestamp)
    val status: SourceImageStatus,
    val retryCount: Int,
    val lastError: String?       // 最近一次解析錯誤訊息
)

enum class SourceImageStatus {
    PENDING_OCR,   // 尚未送後端 / 等待解析
    PROCESSING,    // 後端正在解析
    READY,         // 已解析出 transactions，對應的 PaymentRecord 已建立
    FAILED         // 多次重試後仍失敗
}
```

> 注意：`localPath` 存在 app 專用目錄，不註冊 MediaStore，因此系統相簿不會顯示。[^4][^3]

### 2.2 `PaymentRecord`（單筆消費記錄）

```kotlin
data class PaymentRecord(
    val id: String,              // UUID
    val sourceImageId: String,   // 關聯到對應的 SourceImage
    val method: String,          // 例如 "一般刷卡", "Line Pay", "全支付", "街口", "悠遊付", "現金", "未知"
    val account: String?,        // 實際刷的信用卡名稱，例如 "台新Rose信用卡"，非電子支付名稱
    val cardLast4: String?,      // 信用卡末四碼，例如 "1234"
    val amount: Double,          // 金額
    val currency: String?,       // 幣別，例如 "TWD", "USD"
    val consumeDate: String?,    // "YYYY/MM/DD"
    val description: String?,    // 商家名稱或說明
    val status: PaymentStatus,
    val createdAt: Long,
    val approvedAt: Long?
)

enum class PaymentStatus {
    READY_FOR_APPROVAL,  // 已有解析結果，等待使用者核准
    APPROVED             // 已核准，且已送至 n8n webhook
}
```


### 2.3 電子支付／信用卡維護資料（略）

延用前面設計：

- `CreditCard`：`id`, `name`, `fullCardNumber`（選填）, `last4`, `issuer`, `isActive`。
- `EWalletAccount`：`id`, `name`, `keywords: List<String>`, `isActive`。

***

## 3. 後端資料與狀態管理

後端主要是 stateless API，但可選擇用輕量 DB（例如 SQLite / Postgres）記：

- LLM 呼叫節流狀態：`lastCallAt`。
- Agnes JSON schema 版本支援：目前只支援 `schema_version = 1`。

如要更嚴謹，也可以記一份 `ReceiptJob` 表，用來對應 Android 上傳圖片與解析狀態，不過 MVP 可先以檔名＋返回 JSON 即可。

***

## 4. API 設計

### 4.1 `/api/receipt/parse`（Android → 後端 → Agnes）

**目的：**
Android app 上傳截圖（壓縮後）＋電子支付帳戶清單，由後端呼叫 Agnes 解析成 JSON，然後把結果回給 Android。

**Request：**

- Method：`POST`
- Content-Type：`multipart/form-data`（推薦）
- 欄位：
    - `image`: file，JPEG，長邊 1080。
    - `ewalletAccounts`: JSON 字串，格式如下：

```json
{
  "ewalletAccounts": [
    {
      "id": "ew1",
      "name": "Line Pay",
      "keywords": ["Line Pay", "LINEPAY", "LP"]
    },
    {
      "id": "ew2",
      "name": "全支付",
      "keywords": ["全支付", "PXPayPlus"]
    }
  ]
}
```

- （可選）標頭：
    - `X-Client-Schema-Version: 1`（方便後端知道 app 端期待的 schema 版本）。

**Response（成功）：**

```json
{
  "schema_version": 1,
  "transactions": [
    {
      "method": "Line Pay",
      "account": "台新Rose信用卡",
      "card_last4": "1234",
      "amount": 200,
      "currency": "TWD",
      "date": "2026/08/22",
      "description": "星巴克早餐咖啡"
    },
    {
      "method": "一般刷卡",
      "account": "永豐信用卡",
      "card_last4": "5678",
      "amount": 1500,
      "currency": "USD",
      "date": "2026/08/23",
      "description": "Amazon 訂單"
    }
  ],
  "confidence": 0.93
}
```

**Response（失敗）：**

- HTTP 500 或 4xx，body 含錯誤代碼與訊息。
- Android 收到錯誤時，不直接建立 `PaymentRecord`，而是更新 `SourceImage` 為 `FAILED` 或留待後端重試。

***

## 5. Agnes 2.0 Flash 整合與節流

### 5.1 影像上傳與 public image URL

1. 收到 `/api/receipt/parse` 的 multipart 圖片。
2. 後端將圖片存到本機目錄，例如：
    - 實體路徑：`/var/www/receipts/{uuid}.jpg`
    - public URL：`https://img.leohu.ddns.net/receipts/{uuid}.jpg`
    - `img.leohu.ddns.net` 使用 Cloudflare DNS + proxy，指向你的自架 HTTP 靜態檔伺服器。Cloudflare Free 適合這種靜態圖片使用情境。[^1][^2]
3. 生成 image URL 後，在 Agnes request 中使用：

```json
{
  "messages": [
    {
      "role": "system",
      "content": "（前面設計好的 Vision LLM system prompt）"
    },
    {
      "role": "user",
      "content": [
        { "type": "text", "text": "（前面 user prompt）" },
        { "type": "image_url", "image_url": "https://img.leohu.ddns.net/receipts/{uuid}.jpg" }
      ]
    }
  ],
  "response_format": { "type": "json_schema", "schema": { ... } } // 若 Agnes 支援
}
```

4. Agnes 回傳 JSON 後，後端可選擇：
    - 立即刪除 `/var/www/receipts/{uuid}.jpg`（短命 public URL）。
    - 或保留一段時間以便除錯，再透過排程清理舊檔。

> Android 端顯示原圖使用的是自己的 `localPath`，不依賴後端這份副本，因此後端副本可以短期存在即可。

### 5.2 節流：每 30 秒一次

後端透過以下方式實作節流：

- 在資料庫或設定檔中維護一個 `lastCallAt` timestamp。
- 在對 Agnes 的呼叫函式前加入：

```pseudo
function callAgnes(request):
    now = current_timestamp()
    if lastCallAt != null and (now - lastCallAt) < 30秒:
        sleep(30秒 - (now - lastCallAt))
    // 呼叫 Agnes
    response = agnesApiRequest(request)
    lastCallAt = current_timestamp()
    return response
```

- 讓 `/api/receipt/parse` 內部呼叫 Agnes 的地方都走這個函式。
- 建議後端只有一個負責 Agnes 的 worker / thread，以免多個並發不小心繞過節流。


### 5.3 JSON schema 驗證＋版本控管

- 定義 `schema_version = 1` 的 JSON schema（例如用 Pydantic / Zod）。
- 當 Agnes 回應時：

1. 檢查 `schema_version`（若沒有就視為錯誤）。
2. 用 v1 schema 驗證：欄位型別、必填欄位、陣列結構等。
3. 驗證失敗 → 視為失敗，進入錯誤重試流程（不回成功給 Android）。
- 未來若 schema 要擴充：
    - Agnes 回傳 `schema_version = 2`。
    - 後端同時支援 v1 / v2 validator；Android 可用 header 告知自己預期版本。

***

## 6. 錯誤重試與狀態更新流程

### 6.1 後端重試邏輯

在 `/api/receipt/parse` 內部：

1. 解析 multipart，存 image → 生成 URL。
2. 呼叫 `callAgnes(request)`（包含節流）。
3. 驗證 JSON：
    - 若成功 → 回傳 JSON 給 Android。
    - 若失敗：
        - 回傳錯誤（例如 HTTP 500 + error code），讓 Android 可以標記 `SourceImage` 為 `FAILED` 或給使用者提示。
        - 或在後端自己實作 job queue，針對某張圖重試 N 次後才回傳「最終失敗」。

你之前已要求「錯誤重試」，這裡統一做法是：

- **後端**負責多次呼叫 Agnes 重試＋節流。
- **Android**只看到最終結果（成功或失敗），若失敗可顯示「解析失敗，可重試」。


### 6.2 Android 端狀態更新

- 成功時：
    - 從 `schema_version=1` JSON 中讀出 `transactions[]`。
    - 為每一個 transaction 建立 `PaymentRecord`，`sourceImageId` 指向該 `SourceImage.id`。
    - 將 `SourceImage.status = READY`。
- 失敗時：
    - 可將 `SourceImage.status = FAILED`，並顯示錯誤訊息／重試按鈕。
    - 按重試時，重新送同一張圖到 `/api/receipt/parse`。

***

## 7. 圖片壓縮與 Android 檔案處理

### 7.1 圖片壓縮（長邊 1080）

- 在 Share Activity 或背景 worker 中：
    - 取得分享過來的 `content://` URI。
    - 使用 `BitmapFactory` 讀入原圖。
    - 計算縮放比例，使長邊 = 1080（寬或高任一）。
    - 使用 `Bitmap.createScaledBitmap` 生成縮圖。
    - 以 JPEG（品質 ~80）寫入 app 專用目錄：
        - 例如 `filesDir/receipts/{uuid}.jpg`。
- 優點：
    - 上傳給後端的檔案較小，節省網路與後端存儲。
    - Vision LLM 對收據截圖通常不需要高於 1080 的解析度即可正確解析。


### 7.2 不註冊 MediaStore

- 寫檔時只用 `FileOutputStream` 到 app 私有路徑，不呼叫任何 MediaStore insert API。
- 也不對該檔案執行 `MediaScannerConnection.scanFile`。
- 系統相簿只會掃描公共儲存路徑（例如 DCIM, Pictures），因此不會顯示這些檔案。

***

## 8. 與 n8n Webhook 的整合（含 currency）

核准時，Android 端將已編輯／確認的 `PaymentRecord` 組成 GET URL 呼叫 n8n webhook，例如：

```text
GET http://leohu.ddns.net:5678/webhook/ExpenseCheck3
    ?method=Line%20Pay
    &account=台新Rose信用卡
    &amount=200
    &description=星巴克早餐咖啡
    &date=2026/08/22
    &currency=TWD
```

- `method`：照 `PaymentRecord.method`。
- `account`：照 `PaymentRecord.account`（信用卡名稱）。
- `amount`：照 `PaymentRecord.amount`。
- `description`：照 `PaymentRecord.description`。
- `date`：照 `PaymentRecord.consumeDate`。
- `currency`：照 `PaymentRecord.currency`。

n8n 與 DB 端只需擴充一個 `currency` 欄位即可。

***

## 9. 後續 Coding Agent 的工作切分建議

你可以把這份設計文件交給不同 Agent／開發者，分工如下：

1. **Android Agent**
    - 實作 Share Activity 接收截圖。
    - 實作圖片壓縮（長邊 1080）＋ app 專用目錄存檔。
    - 設計 Room entity：`SourceImage`, `PaymentRecord`, `CreditCard`, `EWalletAccount`。
    - 實作背景 worker：
        - 呼叫 `/api/receipt/parse` 上傳圖片＋電子支付清單。
        - 接收 JSON、建立 `PaymentRecord`。
    - 實作 UI：
        - 待核准列表、單筆編輯＋顯示原圖。
        - 核准後呼叫 n8n webhook（含 currency）。
2. **後端 Agent**
    - 架設 API 伺服器（Python / Node.js 任一）。
    - 實作 `/api/receipt/parse`：
        - 接收 multipart 圖片＋ `ewalletAccounts` JSON。
        - 寫入 `/var/www/receipts/{uuid}.jpg`。
        - 生成 `https://img.leohu.ddns.net/receipts/{uuid}.jpg`。
        - 呼叫 Agnes 2.0 Flash（包含我們寫好的 system/user prompt）。
        - 驗證 JSON schema_version=1 並回傳給 Android。
    - 實作節流（30 秒一次）＋錯誤重試。
    - 配置 Nginx / Caddy 靜態檔伺服器，搭配 Cloudflare Free DNS/CDN。[^2][^1]
3. **Ops / Infra Agent（可選）**
    - 設定 Cloudflare DNS（`img.leohu.ddns.net` 指向你的 DDNS）。
    - 設定 HTTPS 憑證（Let’s Encrypt 或 Cloudflare）。

***
