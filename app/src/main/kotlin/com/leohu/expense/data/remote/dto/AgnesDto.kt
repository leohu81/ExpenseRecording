package com.leohu.expense.data.remote.dto

import com.leohu.expense.domain.model.EWalletAccount

data class AgnesRequest(
    val model: String = "agnes-2.0-flash",
    val messages: List<AgnesMessage>,
    val max_tokens: Int = 4096,
    val temperature: Float = 0.0f
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
    val url: String // Data URI base64
)

data class AgnesResponse(
    val choices: List<AgnesChoice>,
    val usage: AgnesUsage?
)

data class AgnesChoice(
    val message: AgnesResponseMessage
)

data class AgnesMessageSimple(
    val role: String,
    val content: String
)

data class AgnesResponseMessage(
    val role: String,
    val content: String
)

data class AgnesUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class TransactionDto(
    val method: String,
    val account: String?,
    val card_last4: String?,
    val amount: Double,
    val currency: String?,
    val date: String?,
    val description: String?
)

data class AgnesResponseDto(
    val schema_version: Int,
    val transactions: List<TransactionDto>,
    val confidence: Double?
)
