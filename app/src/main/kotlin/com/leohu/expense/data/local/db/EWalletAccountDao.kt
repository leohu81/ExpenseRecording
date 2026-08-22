package com.leohu.expense.data.local.db

import androidx.room.*
import com.leohu.expense.data.local.entity.EWalletAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EWalletAccountDao {
    @Query("SELECT * FROM ewallet_accounts WHERE isActive = 1")
    fun getAllActive(): Flow<List<EWalletAccountEntity>>

    @Query("SELECT * FROM ewallet_accounts")
    suspend fun getAll(): List<EWalletAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EWalletAccountEntity)

    @Update
    suspend fun update(entity: EWalletAccountEntity)

    @Delete
    suspend fun delete(entity: EWalletAccountEntity)
}
