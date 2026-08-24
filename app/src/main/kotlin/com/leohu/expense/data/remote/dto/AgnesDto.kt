package com.leohu.expense.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.leohu.expense.domain.model.EWalletAccount

data class AgnesRequest(
    @SerializedName("model") val model: String = "agnes-2.5-flash",
    @SerializedName("messages") val messages: List<AgnesMessage>,
    @SerializedName("max_tokens") val max_tokens: Int = 4096,
    @SerializedName("temperature") val temperature: Float = 0.0f
)

data class AgnesMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: List<AgnesContent>
)

data class AgnesContent(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val image_url: AgnesImageUrl? = null
)

data class AgnesImageUrl(
    @SerializedName("url") val url: String // Data URI base64
)

data class AgnesResponse(
    @SerializedName("choices") val choices: List<AgnesChoice>,
    @SerializedName("usage") val usage: AgnesUsage?
)

data class AgnesChoice(
    @SerializedName("message") val message: AgnesResponseMessage
)

data class AgnesMessageSimple(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class AgnesResponseMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class AgnesUsage(
    @SerializedName("prompt_tokens") val prompt_tokens: Int,
    @SerializedName("completion_tokens") val completion_tokens: Int,
    @SerializedName("total_tokens") val total_tokens: Int
)

data class TransactionDto(
    @SerializedName("method") val method: String,
    @SerializedName("account") val account: String?,
    @SerializedName("card_last4") val card_last4: String?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("description") val description: String?
)

data class AgnesResponseDto(
    @SerializedName("schema_version") val schema_version: Int,
    @SerializedName("transactions") val transactions: List<TransactionDto>,
    @SerializedName("confidence") val confidence: Double?
)
