## 一、專案準備與基礎結構

### Task 1：建立資料 model（domain）

**內容：**

- 在 `com.leohu.expense.domain.model` 建立：
    - `SourceImage`
    - `PaymentRecord`
    - `CreditCard`
    - `EWalletAccount`

**給 Gemini 的指令範本：**

> 參考 `docs/expense-design.md` 中的資料模型描述，幫我在 `com.leohu.expense.domain.model` 建立 `SourceImage`, `PaymentRecord`, `CreditCard`, `EWalletAccount` 的 Kotlin data class。
> 請不要依賴 Android framework，純 Kotlin 即可。

***

### Task 2：建立 Room entity + DAO + AppDatabase

**內容：**

- 在 `data.local.entity` 建立對應的 Room entity。
- 在 `data.local.db` 建立：
    - `AppDatabase`
    - `SourceImageDao`, `PaymentRecordDao`, `CreditCardDao`, `EWalletAccountDao`

**指令範本：**

> 根據 domain model 和設計文件，幫我建立 Room entity 與 DAO：
> - package: `com.leohu.expense.data.local.entity`
> - package: `com.leohu.expense.data.local.db`
> 建立 `AppDatabase` 以及四個 DAO。
> 請使用目前 Gradle 中設定的 Room 版本，並對應 `SourceImageStatus` / `PaymentStatus` 等列舉。

***

## 二、Repository 與 UseCase

### Task 3：實作 `ExpenseRepository`

**內容：**

- interface：`com.leohu.expense.domain.repository.ExpenseRepository`
- 實作：`com.leohu.expense.data.repository.ExpenseRepositoryImpl`

主要方法：

- `enqueueSourceImage(uri: Uri)`
- `getPendingImages()` / `getReadyImages()`
- `createPaymentRecordsForImage(imageId: String, transactions: List<TransactionDto>)`
- `getPendingApprovalRecords()` / `approvePaymentRecord(recordId: String)`
- `getCards()` / `getEWalletAccounts()` 等。

**指令範本：**

> 請根據設計文件中的流程與資料模型，在
> - `com.leohu.expense.domain.repository` 建立 `ExpenseRepository` interface
> - `com.leohu.expense.data.repository` 建立 `ExpenseRepositoryImpl` 實作
> Repository 要封裝 Room 存取與後端 API 呼叫，不要讓 ViewModel 直接操作 DAO 或 Retrofit。

***

### Task 4：建立主要 UseCase

**內容：**

- 在 `domain.usecase` 建立：
    - `EnqueueSourceImageUseCase`
    - `ParseSourceImageUseCase`
    - `CreatePaymentRecordsUseCase`
    - `ApprovePaymentRecordUseCase`
    - `RetryFailedImageUseCase`
    - `CleanupOldApprovedRecordsUseCase`

**指令範本：**

> 依照設計文件中的 UseCase 列表，幫我在 `com.leohu.expense.domain.usecase` 建立對應的 UseCase 類別。
> 每個 UseCase 接收必要的參數與 `ExpenseRepository`，負責一個清楚的動作，例如 enqueue 圖片、對某張圖片發動解析、核准某筆 PaymentRecord 等。

***

## 三、背景 Worker 與 Share 接收

### Task 5：實作 `ShareReceiveActivity`

**內容：**

- 在 `com.leohu.expense.share.ShareReceiveActivity` 內：
    - 處理 `ACTION_SEND` / `ACTION_SEND_MULTIPLE` image URI。
    - 呼叫 `EnqueueSourceImageUseCase` 建立 `SourceImage`。
    - 啟動 `ImageCompressWorker`。

**指令範本：**

> 請在 `ShareReceiveActivity` 中實作分享截圖接收流程：
> - 接收 `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 的 image intent。
> - 讀取 URI，交給 `EnqueueSourceImageUseCase` 建立 `SourceImage`。
> - 排程一個 `ImageCompressWorker` 對這些 image 進行壓縮並存到 app 專用目錄。
> 完成後直接 `finish()` 不顯示 UI。

***

### Task 6：實作 `ImageCompressWorker`

**內容：**

- 壓縮圖片長邊為 1080。
- 存到 app 專用目錄，更新 `SourceImage.localPath`。

**指令範本：**

> 在 `com.leohu.expense.worker` 建立 `ImageCompressWorker`：
> - 從 `SourceImage` 取得原始 URI。
> - 壓縮圖片長邊為 1080（JPEG，品質約 80）。
> - 存到 app 專用目錄 `/files/receipts/{uuid}.jpg`，更新對應的 `SourceImage.localPath`。
> 請使用 WorkManager 的 `CoroutineWorker` 實作。

***

### Task 7：實作 `UploadAndParseWorker`

**內容：**

- 將 `SourceImage.localPath` 對應的檔案上傳到後端 `/api/receipt/parse`。
- 帶入電子支付帳戶清單 JSON。
- 接收 Agnes 解析結果，建立 `PaymentRecord`。

**指令範本：**

> 在 `com.leohu.expense.worker` 建立 `UploadAndParseWorker`：
> - 對 status = `PENDING_OCR` 的 `SourceImage` 逐一處理。
> - 將圖片檔與電子支付帳戶清單上傳到 `/api/receipt/parse`。
> - 解析回應 JSON（含 `schema_version`, `transactions[]`），呼叫 UseCase 建立多筆 `PaymentRecord`，並將 `SourceImage.status` 改為 `READY`。
> Worker 本身不直接呼叫 Agnes，而是呼叫你的後端 API。

***

## 四、UI：列表與編輯核准

### Task 8：Home / Queue / Approval 列表畫面

**內容：**

- `HomeScreen` + `HomeViewModel`：顯示待處理、待核准、已核准數量。
- `ApprovalListScreen` + `ApprovalViewModel`：顯示 `READY_FOR_APPROVAL` 的 `PaymentRecord` 列表。
- `HistoryScreen`：顯示已核准記錄。

**指令範本：**

> 依照設計文件中的首頁與列表 wireframe，在
> `com.leohu.expense.ui.feature.home` 建立 `HomeScreen` / `HomeViewModel`，
> 在 `feature.approval` 建立 `ApprovalListScreen` / `ApprovalViewModel`，
> 在 `feature.history` 建立 `HistoryScreen`。
> ViewModel 使用 UseCase 取得對應資料，Compose 畫面依簡單列表樣式顯示。

***

### Task 9：單筆編輯 / 核准畫面

**內容：**

- `ApprovalDetailScreen`：顯示單筆 `PaymentRecord`＋原始截圖。
- 支援修改 `method`, `account`, `amount`, `currency`, `date`, `description`。
- 「核准」按鈕會呼叫 `ApprovePaymentRecordUseCase` → n8n webhook。

**指令範本：**

> 在 `com.leohu.expense.ui.feature.approval` 建立 `ApprovalDetailScreen`，
> 顯示指定 `PaymentRecord` 的所有欄位與其 `SourceImage` 對應的截圖。
> 畫面上提供下拉選單修改 `method`（一般刷卡 / Line Pay / 全支付等）與 `account`（信用卡名稱列表），還有文字欄位修改金額、日期、description。
> 點擊「核准」按鈕後呼叫 `ApprovePaymentRecordUseCase`，再由 Repository 發送 n8n webhook。

***

## 五、維護介面與設定

### Task 10：信用卡維護 UI

**內容：**

- `CardListScreen` / `CardViewModel`。
- 新增／編輯／刪除 `CreditCard`。

**指令範本：**

> 在 `ui.feature.cards` 實作信用卡維護畫面：
> - 列出所有 `CreditCard`。
> - 提供新增／編輯／刪除功能。
> - 透過 Repository 操作 Room 中的 `CreditCardEntity`。

***

### Task 11：電子支付帳戶維護 UI

**內容：**

- `EWalletListScreen` / `EWalletViewModel`。
- 維護 `EWalletAccount` 名稱與 keywords。

**指令範本：**

> 在 `ui.feature.ewallets` 實作電子支付帳戶維護畫面：
> - 列出所有 `EWalletAccount`。
> - 新增／編輯帳戶名稱與 keywords（以逗號分隔）。
> - 儲存到 Room；後端呼叫 `/api/receipt/parse` 時也可以從 Repository 取得這份清單組成 JSON。

***

### Task 12：設定／TTL 清理 UI

**內容：**

- `SettingsScreen` / `SettingsViewModel`。
- 控制已核准記錄的保留天數、是否自動刪除。
- 透過 `CleanupOldApprovedRecordsUseCase` 跑刪除。

**指令範本：**

> 在 `ui.feature.settings` 實作設定畫面：
> - 使用者可以設定已核准記錄保留天數（例如 90）。
> - 提供「立即批次刪除超過保留天數的已核准記錄」按鈕。
> - 呼叫 `CleanupOldApprovedRecordsUseCase` 執行刪除，並將相關 `SourceImage` 是否可刪除一併處理。

***
