package com.leohu.expense.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.leohu.expense.data.local.entity.CreditCardEntity
import com.leohu.expense.data.local.entity.EWalletAccountEntity
import com.leohu.expense.data.local.entity.PaymentRecordEntity
import com.leohu.expense.data.local.entity.SourceImageEntity

@Database(
    entities = [
        SourceImageEntity::class,
        PaymentRecordEntity::class,
        CreditCardEntity::class,
        EWalletAccountEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceImageDao(): SourceImageDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun eWalletAccountDao(): EWalletAccountDao
}
