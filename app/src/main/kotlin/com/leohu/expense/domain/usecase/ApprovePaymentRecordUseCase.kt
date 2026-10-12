package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.util.PreferenceHelper

class ApprovePaymentRecordUseCase(
    private val repository: ExpenseRepository,
    private val preferenceHelper: PreferenceHelper
) {
    suspend operator fun invoke(recordId: String, syncWithServer: Boolean = true) {
        val record = repository.getPaymentRecordById(recordId) ?: return
        if (record.status == PaymentStatus.READY_FOR_APPROVAL) {
            // Update status to APPROVED first
            val approvedRecord = record.copy(
                status = PaymentStatus.APPROVED,
                approvedAt = System.currentTimeMillis()
            )
            repository.updatePaymentRecord(approvedRecord)
            
            // If cloud mode is enabled and sync is requested, sync to n8n webhook
            if (syncWithServer && preferenceHelper.getStorageMode() == PreferenceHelper.MODE_CLOUD) {
                val baseUrl = preferenceHelper.getBaseUrl()
                if (baseUrl.isNotEmpty()) {
                    repository.syncToN8nInsert(approvedRecord, baseUrl)
                }
            }
        }
    }
}
