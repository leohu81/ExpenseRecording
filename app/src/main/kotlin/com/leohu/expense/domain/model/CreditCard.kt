package com.leohu.expense.domain.model

data class CreditCard(
    val id: String,
    val name: String,
    val fullCardNumber: String? = null,
    val last4: String?,
    val issuer: String?,
    val isActive: Boolean = true
)
