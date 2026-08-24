package com.leohu.expense.domain.model

data class SourceImage(
    val id: String,              // UUID
    val localPath: String,
    val createdAt: Long,
    val status: SourceImageStatus,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val preDescription: String? = null,
    val tags: List<String> = emptyList() // 新增：預設標籤
)

enum class SourceImageStatus {
    PENDING_OCR,  // 等待壓縮或準備傳送
    PROCESSING,   // 正在呼叫 LLM
    READY,        // 已解析完成，對應 PaymentRecord 狀態為 READY_FOR_APPROVAL
    FAILED        // 解析失敗
}
