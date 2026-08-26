package com.leohu.expense.data.remote.dto

data class AgnesRequest(
    val model: String = "agnes-2.5-flash",
    val messages: List<AgnesMessage>,
    val temperature: Double = 0.0,
    val max_tokens: Int = 4096
)

data class AgnesMessage(
    val role: String,
    val content: List<AgnesContent>
)

data class AgnesContent(
    val type: String,
    val text: String? = null,
    val image_url: AgnesImageUrl? = null
)

data class AgnesImageUrl(
    val url: String
)

data class AgnesResponse(
    val choices: List<AgnesChoice>
)

data class AgnesChoice(
    val message: AgnesChoiceMessage
)

data class AgnesChoiceMessage(
    val content: String
)

data class AgnesResponseDto(
    val schema_version: Int,
    val transactions: List<AgnesTransactionDto>,
    val confidence: Double
)

data class AgnesTransactionDto(
    val method: String,
    val account: String?,
    val card_last4: String?,
    val amount: Double,
    val amount_twd: Double?, // 新增：約當台幣
    val currency: String,
    val date: String,
    val description: String
)
