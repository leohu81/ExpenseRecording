package com.leohu.expense.data.local.db

import androidx.room.*
import com.leohu.expense.data.local.entity.SourceImageEntity
import com.leohu.expense.domain.model.SourceImageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceImageDao {
    @Query("SELECT * FROM source_images WHERE id = :id")
    suspend fun getById(id: String): SourceImageEntity?

    @Query("SELECT * FROM source_images WHERE status = :status")
    fun getByStatus(status: SourceImageStatus): Flow<List<SourceImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SourceImageEntity)

    @Update
    suspend fun update(entity: SourceImageEntity)

    @Delete
    suspend fun delete(entity: SourceImageEntity)
}
