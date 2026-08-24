package com.leohu.expense.data.local.db

import androidx.room.*
import com.leohu.expense.data.local.entity.PaymentRecordTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRecordTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PaymentRecordTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<PaymentRecordTagEntity>)

    @Delete
    suspend fun delete(entity: PaymentRecordTagEntity)

    @Query("DELETE FROM payment_record_tags WHERE paymentRecordId = :paymentRecordId")
    suspend fun deleteAllByPaymentRecordId(paymentRecordId: String)

    @Query("SELECT * FROM payment_record_tags WHERE paymentRecordId = :paymentRecordId")
    fun getTagsForPaymentRecord(paymentRecordId: String): Flow<List<PaymentRecordTagEntity>>

    @Query("SELECT * FROM payment_record_tags WHERE tagId = :tagId")
    fun getPaymentRecordsWithTag(tagId: String): Flow<List<PaymentRecordTagEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM payment_record_tags WHERE paymentRecordId = :paymentRecordId AND tagId = :tagId)")
    suspend fun isAssociated(paymentRecordId: String, tagId: String): Boolean
}