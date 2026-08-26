package com.leohu.expense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leohu.expense.domain.model.PaymentStatus

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val sourceImageId: String,
    val method: String,
    val account: String?,
    val cardLast4: String?,
    val amount: Double,
    val amountTwd: Double?, // 新增：約當台幣
    val currency: String?,
    val consumeDate: String?,
    val description: String?,
    val status: PaymentStatus,
    val tags: String?,
    val createdAt: Long,
    val approvedAt: Long?
)
