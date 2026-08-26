package com.leohu.expense.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.leohu.expense.data.local.entity.*

@Database(
    entities = [
        SourceImageEntity::class,
        PaymentRecordEntity::class,
        CreditCardEntity::class,
        EWalletAccountEntity::class,
        TagEntity::class,
        PaymentRecordTagEntity::class
    ],
    version = 4, // 升級至 v4
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceImageDao(): SourceImageDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun eWalletAccountDao(): EWalletAccountDao
    abstract fun tagDao(): TagDao
    abstract fun paymentRecordTagDao(): PaymentRecordTagDao
}
