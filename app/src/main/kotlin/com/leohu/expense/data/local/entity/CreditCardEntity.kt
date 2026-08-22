package com.leohu.expense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fullCardNumber: String?,
    val last4: String?,
    val issuer: String?,
    val isActive: Boolean
)
