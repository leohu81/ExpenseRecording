package com.leohu.expense.domain.repository

import com.leohu.expense.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    // SourceImage
    suspend fun enqueueSourceImage(localPath: String): String
    fun getSourceImagesByStatus(status: SourceImageStatus): Flow<List<SourceImage>>
    suspend fun getSourceImageById(id: String): SourceImage?
    suspend fun updateSourceImage(image: SourceImage)
    
    // PaymentRecord
    fun getPaymentRecordsByStatus(status: PaymentStatus): Flow<List<PaymentRecord>>
    suspend fun getPaymentRecordById(id: String): PaymentRecord?
    fun getPaymentRecordByIdFlow(id: String): Flow<PaymentRecord?>
    suspend fun createPaymentRecords(imageId: String, transactions: List<PaymentRecord>)
    suspend fun approvePaymentRecord(recordId: String)
    suspend fun updatePaymentRecord(record: PaymentRecord)
    suspend fun deletePaymentRecord(record: PaymentRecord)
    suspend fun deleteOldApprovedRecords(days: Int)
    suspend fun sendToWebhook(record: PaymentRecord)

    // Maintenance
    fun getCreditCards(): Flow<List<CreditCard>>
    suspend fun addCreditCard(card: CreditCard)
    suspend fun deleteCreditCard(card: CreditCard)

    fun getEWalletAccounts(): Flow<List<EWalletAccount>>
    suspend fun getAllEWalletAccounts(): List<EWalletAccount>
    suspend fun addEWalletAccount(account: EWalletAccount)
    suspend fun deleteEWalletAccount(account: EWalletAccount)
}
