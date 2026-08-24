package com.leohu.expense.data.local.db

import androidx.room.*
import com.leohu.expense.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TagEntity)

    @Delete
    suspend fun delete(entity: TagEntity)

    @Update
    suspend fun update(entity: TagEntity)

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name")
    suspend fun countByName(name: String): Int
}