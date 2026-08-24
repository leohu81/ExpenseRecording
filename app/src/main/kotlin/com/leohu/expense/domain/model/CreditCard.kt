package com.leohu.expense.domain.model

import com.google.gson.annotations.SerializedName

data class CreditCard(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("fullCardNumber") val fullCardNumber: String? = null,
    @SerializedName("last4") val last4: String?,
    @SerializedName("issuer") val issuer: String?,
    @SerializedName("isActive") val isActive: Boolean = true
)
