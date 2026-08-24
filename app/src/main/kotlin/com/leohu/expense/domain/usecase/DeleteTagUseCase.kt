package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.Tag
import com.leohu.expense.domain.repository.ExpenseRepository

class DeleteTagUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(tag: Tag) {
        repository.deleteTag(tag)
    }
}
