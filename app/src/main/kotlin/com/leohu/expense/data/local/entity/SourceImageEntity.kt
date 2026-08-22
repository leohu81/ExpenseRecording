package com.leohu.expense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leohu.expense.domain.model.SourceImageStatus

@Entity(tableName = "source_images")
data class SourceImageEntity(
    @PrimaryKey val id: String,
    val localPath: String,
    val createdAt: Long,
    val status: SourceImageStatus,
    val retryCount: Int,
    val lastError: String?
)
