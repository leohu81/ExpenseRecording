package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.Tag
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class TagManagerUseCase(private val repository: ExpenseRepository) {
    fun getAllTags(): Flow<List<Tag>> = repository.getAllTags()
    
    suspend fun addTag(name: String, color: String = "#FFF9C4"): Tag {
        val id = java.util.UUID.randomUUID().toString()
        val tag = Tag(id = id, name = name, color = color)
        repository.addTag(tag)
        return tag
    }
    
    suspend fun getTagById(id: String): Tag? = repository.getTagById(id)
}
