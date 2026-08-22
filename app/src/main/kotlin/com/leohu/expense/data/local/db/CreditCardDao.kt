package com.leohu.expense.data.local.db

import androidx.room.*
import com.leohu.expense.data.local.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards WHERE isActive = 1")
    fun getAllActive(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards")
    suspend fun getAll(): List<CreditCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CreditCardEntity)

    @Update
    suspend fun update(entity: CreditCardEntity)

    @Delete
    suspend fun delete(entity: CreditCardEntity)
}
