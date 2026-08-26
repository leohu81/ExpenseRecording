package com.leohu.expense.util

import com.leohu.expense.domain.model.*
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class MockRepository(
    private val images: MutableMap<String, SourceImage> = mutableMapOf(),
    private val records: MutableMap<String, PaymentRecord> = mutableMapOf(),
    private val tags: MutableList<Tag> = mutableListOf(),
    private val cards: MutableList<CreditCard> = mutableListOf(),
    private val ewallets: MutableList<EWalletAccount> = mutableListOf()
) : ExpenseRepository {
    
    // SourceImage operations
    override suspend fun enqueueSourceImage(localPath: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val image = SourceImage(
            id = id,
            localPath = localPath,
            createdAt = System.currentTimeMillis(),
            status = SourceImageStatus.PENDING_OCR
        )
        images[id] = image
        return id
    }
    
    override fun getSourceImagesByStatus(status: SourceImageStatus): Flow<List<SourceImage>> {
        return flow {
            emit(images.values.filter { it.status == status }.toList())
        }
    }
    
    override suspend fun getSourceImageById(id: String): SourceImage? {
        return images[id]
    }
    
    override fun getAllSourceImages(): Flow<List<SourceImage>> {
        return flow { emit(images.values.toList()) }
    }
    
    override suspend fun updateSourceImage(image: SourceImage) {
        images[image.id] = image
    }
    
    override suspend fun deleteSourceImage(id: String) {
        images.remove(id)
    }
    
    // PaymentRecord operations
    override fun getPaymentRecordsByStatus(status: PaymentStatus): Flow<List<PaymentRecord>> {
        return flow {
            emit(records.values.filter { it.status == status }.toList())
        }
    }
    
    override suspend fun getPaymentRecordById(id: String): PaymentRecord? {
        return records[id]
    }
    
    override fun getPaymentRecordByIdFlow(id: String): Flow<PaymentRecord?> {
        return flow { emit(records[id]) }
    }
    
    override suspend fun createPaymentRecords(imageId: String, transactions: List<PaymentRecord>) {
        transactions.forEach { record ->
            records[record.id] = record
        }
    }
    
    override suspend fun approvePaymentRecord(recordId: String) {
        val record = records[recordId]
        if (record != null) {
            records[recordId] = record.copy(
                status = PaymentStatus.APPROVED,
                approvedAt = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun updatePaymentRecord(record: PaymentRecord) {
        records[record.id] = record
    }
    
    override suspend fun deletePaymentRecord(record: PaymentRecord) {
        records.remove(record.id)
    }
    
    override suspend fun deleteOldApprovedRecords(days: Int) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        records.values.filter { 
            it.status == PaymentStatus.APPROVED && 
            (it.approvedAt ?: 0L) < cutoffTime 
        }.forEach { records.remove(it.id) }
    }
    
    override suspend fun sendToWebhook(record: PaymentRecord) {
        // Mock: do nothing
    }
    
    // CreditCard operations
    override fun getCreditCards(): Flow<List<CreditCard>> {
        return flow { emit(cards) }
    }
    
    override suspend fun addCreditCard(card: CreditCard) {
        cards.add(card)
    }
    
    override suspend fun deleteCreditCard(card: CreditCard) {
        cards.remove(card)
    }
    
    // EWallet operations
    override fun getEWalletAccounts(): Flow<List<EWalletAccount>> {
        return flow { emit(ewallets) }
    }
    
    override suspend fun getAllEWalletAccounts(): List<EWalletAccount> {
        return ewallets
    }
    
    override suspend fun addEWalletAccount(account: EWalletAccount) {
        ewallets.add(account)
    }
    
    override suspend fun deleteEWalletAccount(account: EWalletAccount) {
        ewallets.remove(account)
    }
    
    // Tag operations
    override fun getAllTags(): Flow<List<Tag>> {
        return flow { emit(tags) }
    }
    
    override suspend fun getTagById(id: String): Tag? {
        return tags.find { it.id == id }
    }
    
    override suspend fun addTag(tag: Tag) {
        tags.add(tag)
    }
    
    override suspend fun deleteTag(tag: Tag) {
        tags.remove(tag)
    }
    
    override suspend fun updateTag(tag: Tag) {
        val index = tags.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            tags[index] = tag
        }
    }
    
    override suspend fun getTagsForPaymentRecord(paymentRecordId: String): Flow<List<Tag>> {
        val record = records[paymentRecordId]
        return flow {
            emit(record?.tags?.mapNotNull { tagId -> tags.find { it.id == tagId } } ?: emptyList())
        }
    }
    
    override suspend fun addTagToPayment(paymentRecordId: String, tagId: String) {
        val record = records[paymentRecordId]
        if (record != null) {
            val updatedTags = record.tags + tagId
            records[paymentRecordId] = record.copy(tags = updatedTags)
        }
    }
    
    override suspend fun removeTagFromPayment(paymentRecordId: String, tagId: String) {
        val record = records[paymentRecordId]
        if (record != null) {
            val updatedTags = record.tags - tagId
            records[paymentRecordId] = record.copy(tags = updatedTags)
        }
    }
}
