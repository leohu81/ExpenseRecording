package com.leohu.expense.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "payment_record_tags",
    primaryKeys = ["paymentRecordId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = PaymentRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentRecordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["paymentRecordId"]), Index(value = ["tagId"])]
)
data class PaymentRecordTagEntity(
    val paymentRecordId: String,
    val tagId: String
)
