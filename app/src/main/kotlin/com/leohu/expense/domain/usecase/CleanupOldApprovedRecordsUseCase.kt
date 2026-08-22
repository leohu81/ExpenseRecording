package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.repository.ExpenseRepository

class CleanupOldApprovedRecordsUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(days: Int) {
        repository.deleteOldApprovedRecords(days)
    }
}
