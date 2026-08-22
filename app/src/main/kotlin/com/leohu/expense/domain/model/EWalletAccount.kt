package com.leohu.expense.domain.model

data class EWalletAccount(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val isActive: Boolean = true
)
