package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.repository.ExpenseRepository

class EnqueueSourceImageUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(uri: String): String {
        return repository.enqueueSourceImage(uri)
    }
}
