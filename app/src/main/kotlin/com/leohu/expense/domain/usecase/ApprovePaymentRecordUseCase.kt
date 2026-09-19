package com.leohu.expense.domain.usecase

import com.leohu.expense.data.remote.db.PostgreSQLClient
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.util.PreferenceHelper

class ApprovePaymentRecordUseCase(
    private val repository: ExpenseRepository,
    private val preferenceHelper: PreferenceHelper,
    private val pgClient: PostgreSQLClient
) {
    suspend operator fun invoke(recordId: String, syncWithServer: Boolean = true) {
        val record = repository.getPaymentRecordById(recordId) ?: return
        if (record.status == PaymentStatus.READY_FOR_APPROVAL) {
            try {
                // Update status to APPROVED first
                val approvedRecord = record.copy(
                    status = PaymentStatus.APPROVED,
                    approvedAt = System.currentTimeMillis()
                )
                repository.updatePaymentRecord(approvedRecord)
                
                // If cloud mode is enabled and sync is requested, sync to PostgreSQL
                if (syncWithServer && preferenceHelper.getStorageMode() == PreferenceHelper.MODE_CLOUD) {
                    pgClient.insertPaymentRecord(approvedRecord)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
}
