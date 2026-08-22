## 一、Module 結構

- `app`（唯一 Android module）
    - Kotlin + Jetpack Compose + Room + WorkManager
    - 之後若專案變大再考慮拆成多 module（domain / data / feature），目前用一個 module 比較好上手。[^4][^2]

***

## 二、package 根命名

假設包名用：

```text
com.leohu.expense
```

Android Studio 建專案時設定 `applicationId = "com.leohu.expense"`，所有程式碼放在：

```text
app/src/main/kotlin/com/leohu/expense
```


***

## 三、頂層目錄 layout（app/src/main/kotlin）

參考現代 Clean + Jetpack 建議，把頂層按「技術層 + DI + utils」分包：[^2][^3]

```text
com.leohu.expense
├── di              // Hilt / Koin 等 DI 設定
├── data            // 資料層：Room、HTTP、Repository
├── domain          // 商業邏輯：model ＋ usecase
├── ui              // Presentation 層：Compose UI + ViewModel
├── worker          // 背景任務 (WorkManager / Service)
├── app             // Application / MainActivity / navigation host
└── utils           // 共用工具 (日期格式、currency 工具等)
```


***

## 四、各層內部結構

### 4.1 `data` 資料層

```text
com.leohu.expense.data
├── local
│   ├── db               // RoomDatabase, DAO 定義
│   │   ├── AppDatabase.kt
│   │   └── ExpenseDao.kt (含 SourceImageDao, PaymentRecordDao 等)
│   └── entity           // Room entity
│       ├── SourceImageEntity.kt
│       ├── PaymentRecordEntity.kt
│       ├── CreditCardEntity.kt
│       └── EWalletAccountEntity.kt
├── remote
│   ├── api              // Agnes / 後端 / n8n webhook client
│   │   ├── AgnesApi.kt          // Vision LLM 解析 API
│   │   ├── BackendApi.kt        // /api/receipt/parse
│   │   └── N8nWebhookApi.kt     // ExpenseCheck3 webhook
│   └── dto              // 網路 DTO（Agnes JSON, parse API response）
│       ├── ParseReceiptRequestDto.kt
│       ├── ParseReceiptResponseDto.kt
│       └── TransactionDto.kt
└── repository
    ├── ExpenseRepository.kt      // 對 domain 暴露來源不分的介面
    └── ExpenseRepositoryImpl.kt  // 實作：組合 local + remote
```

這樣 domain 對外只看 `ExpenseRepository`，不關心是 Room 還是 Agnes / n8n。[^3][^1]

### 4.2 `domain` 商業邏輯層

```text
com.leohu.expense.domain
├── model
│   ├── SourceImage.kt
│   ├── PaymentRecord.kt
│   ├── CreditCard.kt
│   └── EWalletAccount.kt
└── usecase
    ├── EnqueueSourceImageUseCase.kt       // 分享截圖 → 建立 SourceImage
    ├── ParseSourceImageUseCase.kt         // 呼叫後端 / Agnes 解析
    ├── CreatePaymentRecordsUseCase.kt     // DTO → 多筆 PaymentRecord
    ├── ApprovePaymentRecordUseCase.kt     // 核准 → 呼叫 n8n webhook
    ├── RetryFailedImageUseCase.kt         // 解析失敗重試
    ├── CleanupOldApprovedRecordsUseCase.kt// TTL 刪除已核准記錄
    └── GetExpenseSummaryUseCase.kt        // 統計用（之後擴充）
```

所有 usecase 都純 Kotlin，不直接依賴 Android framework，方便單元測試。[^5][^3]

### 4.3 `ui` Presentation 層（採 feature-based 結構）

採「以 feature 分 package，再在 feature 裡分 screen / viewmodel / components」的做法，比純技術分層更符合實際開發。[^5][^2]

```text
com.leohu.expense.ui
├── theme          // 顏色、字型、Shape 等
│   ├── Color.kt
│   ├── Typography.kt
│   └── Theme.kt
├── components     // 共用 Compose 元件
│   ├── ExpenseCard.kt
│   ├── StatusChip.kt
│   └── ConfirmDialog.kt
├── navigation     // NavHost, route 定義
│   ├── NavGraph.kt
│   └── Destinations.kt
└── feature
    ├── home
    │   ├── HomeScreen.kt
    │   └── HomeViewModel.kt
    ├── queue        // 待處理 / 解析狀態
    │   ├── QueueScreen.kt
    │   └── QueueViewModel.kt
    ├── approval     // 已處理待核准列表 + 單筆編輯
    │   ├── ApprovalListScreen.kt
    │   ├── ApprovalDetailScreen.kt
    │   └── ApprovalViewModel.kt
    ├── cards        // 信用卡維護
    │   ├── CardListScreen.kt
    │   ├── EditCardScreen.kt
    │   └── CardViewModel.kt
    ├── ewallets     // 電子支付帳戶維護
    │   ├── EWalletListScreen.kt
    │   ├── EditEWalletScreen.kt
    │   └── EWalletViewModel.kt
    ├── settings     // TTL / currency 顯示設定等
    │   ├── SettingsScreen.kt
    │   └── SettingsViewModel.kt
    └── history      // 已核准記錄瀏覽/刪除
        ├── HistoryScreen.kt
        └── HistoryViewModel.kt
```


***

## 五、背景任務與分享接收

### 5.1 `app` package

```text
com.leohu.expense.app
├── ExpenseApplication.kt   // Application 類，初始化 DI/WorkManager 等
└── MainActivity.kt         // NavHost 容器活動
```


### 5.2 `worker` package

```text
com.leohu.expense.worker
├── ImageCompressWorker.kt       // 壓縮長邊 1080，存 app 專用目錄
├── UploadAndParseWorker.kt      // 上傳圖片 + 呼叫 /api/receipt/parse + 建 PaymentRecord
└── CleanupWorker.kt             // 定期清除過期已核准記錄與孤兒圖片
```


### 5.3 Share 接收 Activity

建議獨立 package 放在 `ui` 或 `app` 下：

```text
com.leohu.expense.share
└── ShareReceiveActivity.kt
```

職責：

- 接收 `ACTION_SEND` / `ACTION_SEND_MULTIPLE` image intent。
- 將 URI 轉交給 `EnqueueSourceImageUseCase`（經 ViewModel / repository）建立 `SourceImage`，啟動 `ImageCompressWorker`。

***

## 六、DI 與 Utils

### 6.1 `di` package

```text
com.leohu.expense.di
├── AppModule.kt            // Room, Retrofit/HTTP client, Repository 綁定
├── UseCaseModule.kt        // 各種 usecase 綁定
└── WorkerModule.kt         // WorkManager / WorkerFactory 設定
```

可用 Hilt 或 Koin，依 coding Agent 習慣。[^3]

### 6.2 `utils` package

```text
com.leohu.expense.utils
├── DateFormatter.kt        // 字串 ↔ 日期 "YYYY/MM/DD"
├── CurrencyUtils.kt        // 幣別判斷 / 顯示
├── NetworkResult.kt        // 統一封裝網路結果
└── Logging.kt              // log 封裝
```


***

## 七、測試目錄建議

- `app/src/test/java/com/leohu/expense`
    - 測 `domain.model` ＋ `domain.usecase`
    - 測 `data.repository` 行為（可用 fake remote / fake local）
- `app/src/androidTest/java/com/leohu/expense`
    - Espresso 測 UI（home / approval flow）。
    - 如有需要，UI Automator 測「其他支付 app → 分享 → ShareReceiveActivity」流程。[^6][^7][^8][^9]

***

Coding Agent 若照這個骨架建立專案：

1. 先建立 `app` module，設定包名 `com.leohu.expense`。
2. 按上述樹狀結構建立 package。
3. 逐步填入 entity / usecase / repository / ViewModel / Screen。
