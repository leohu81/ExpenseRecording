package com.leohu.expense.data.local.db

import androidx.room.*
import com.leohu.expense.data.local.entity.PaymentRecordEntity
import com.leohu.expense.domain.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payment_records WHERE id = :id")
    suspend fun getById(id: String): PaymentRecordEntity?

    @Query("SELECT * FROM payment_records WHERE id = :id")
    fun getByIdFlow(id: String): Flow<PaymentRecordEntity?>

    @Query("SELECT * FROM payment_records WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: PaymentStatus): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE sourceImageId = :sourceImageId")
    suspend fun getBySourceImageId(sourceImageId: String): List<PaymentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PaymentRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PaymentRecordEntity>)

    @Update
    suspend fun update(entity: PaymentRecordEntity)

    @Delete
    suspend fun delete(entity: PaymentRecordEntity)

    @Query("DELETE FROM payment_records WHERE status = 'APPROVED' AND approvedAt < :threshold")
    suspend fun deleteOldApproved(threshold: Long)
}
