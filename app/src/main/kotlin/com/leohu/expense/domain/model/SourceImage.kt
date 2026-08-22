package com.leohu.expense.domain.model

data class SourceImage(
    val id: String,              // UUID
    val localPath: String,       // app 專用目錄中的檔案路徑
    val createdAt: Long,         // 建立時間 (timestamp)
    val status: SourceImageStatus,
    val retryCount: Int = 0,
    val lastError: String? = null // 最近一次解析錯誤訊息
)

enum class SourceImageStatus {
    PENDING_OCR,   // 尚未送後端 / 等待解析
    PROCESSING,    // 後端正在解析
    READY,         // 已解析出 transactions
    FAILED         // 多次重試後仍失敗
}
