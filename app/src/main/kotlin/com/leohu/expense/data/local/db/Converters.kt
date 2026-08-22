package com.leohu.expense.data.local.db

import androidx.room.TypeConverter
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImageStatus

class Converters {
    @TypeConverter
    fun fromSourceImageStatus(value: SourceImageStatus): String = value.name

    @TypeConverter
    fun toSourceImageStatus(value: String): SourceImageStatus = SourceImageStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = PaymentStatus.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
}
