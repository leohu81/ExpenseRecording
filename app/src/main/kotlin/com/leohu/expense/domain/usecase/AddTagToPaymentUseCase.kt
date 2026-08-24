package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.repository.ExpenseRepository

class AddTagToPaymentUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentRecordId: String, tagId: String) {
        repository.addTagToPayment(paymentRecordId, tagId)
    }
}
