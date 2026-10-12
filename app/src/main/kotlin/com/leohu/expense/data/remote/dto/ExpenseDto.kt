package com.leohu.expense.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpenseDto(
    val method: String,
    val account: String? = null,
    val amount: Double,
    val amount_twd: Double? = null,
    val currency: String? = null,
    val card_last4: String? = null,
    val consume_date: String? = null,
    val description: String? = null,
    val status: String? = null,
    val local_id: String? = null
)
