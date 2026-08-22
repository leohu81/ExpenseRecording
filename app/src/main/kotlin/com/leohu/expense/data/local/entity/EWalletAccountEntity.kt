package com.leohu.expense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ewallet_accounts")
data class EWalletAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val keywords: List<String>,
    val isActive: Boolean
)
