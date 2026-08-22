package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import java.util.UUID

class CreatePaymentRecordsUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(imageId: String, transactions: List<PaymentRecord>) {
        // Ensure imageId is correctly set and status is READY_FOR_APPROVAL
        val recordsWithMetadata = transactions.map { 
            it.copy(
                id = if (it.id.isEmpty()) UUID.randomUUID().toString() else it.id,
                sourceImageId = imageId,
                status = PaymentStatus.READY_FOR_APPROVAL,
                createdAt = System.currentTimeMillis()
            )
        }
        repository.createPaymentRecords(imageId, recordsWithMetadata)
    }
}
