package com.leohu.expense.domain.model

import com.google.gson.annotations.SerializedName

data class EWalletAccount(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("keywords") val keywords: List<String>,
    @SerializedName("isActive") val isActive: Boolean = true
)
