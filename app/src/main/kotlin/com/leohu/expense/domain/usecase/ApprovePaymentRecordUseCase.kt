package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.repository.ExpenseRepository

class ApprovePaymentRecordUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(recordId: String) {
        val record = repository.getPaymentRecordById(recordId) ?: return
        if (record.status == PaymentStatus.READY_FOR_APPROVAL) {
            try {
                // 1. Send to webhook
                repository.sendToWebhook(record)
                
                // 2. Update status in local DB
                val approvedRecord = record.copy(
                    status = PaymentStatus.APPROVED,
                    approvedAt = System.currentTimeMillis()
                )
                repository.updatePaymentRecord(approvedRecord)
            } catch (e: Exception) {
                e.printStackTrace()
                // You might want to handle this better, e.g., re-throwing or notifying UI
                throw e 
            }
        }
    }
}
