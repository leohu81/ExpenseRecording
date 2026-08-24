package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.repository.ExpenseRepository

class RemoveTagFromPaymentUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentRecordId: String, tagId: String) {
        repository.removeTagFromPayment(paymentRecordId, tagId)
    }
}
