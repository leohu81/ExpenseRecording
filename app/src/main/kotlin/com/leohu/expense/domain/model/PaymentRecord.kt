package com.leohu.expense.domain.model

data class PaymentRecord(
    val id: String,              // UUID
    val sourceImageId: String,   // 關聯到對應的 SourceImage
    val method: String,          // 例如 "一般刷卡", "Line Pay", "全支付", "街口", "悠遊付", "現金", "未知"
    val account: String?,        // 實際刷的信用卡名稱
    val cardLast4: String?,      // 信用卡末四碼
    val amount: Double,          // 原始金額
    val amountTwd: Double?,      // 新增：約當台幣金額
    val currency: String?,       // 幣別
    val consumeDate: String?,    // "YYYY/MM/DD"
    val description: String?,    // 商家名稱或說明
    val status: PaymentStatus,
    val tags: List<String> = emptyList(),  // Tag ID 列表
    val createdAt: Long,
    val approvedAt: Long? = null
)

enum class PaymentStatus {
    READY_FOR_APPROVAL,  // 已有解析結果，等待使用者核准
    APPROVED             // 已核准
}
