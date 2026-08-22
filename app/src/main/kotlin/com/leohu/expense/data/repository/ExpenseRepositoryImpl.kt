package com.leohu.expense.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.leohu.expense.data.local.db.AppDatabase
import com.leohu.expense.data.mapper.*
import com.leohu.expense.data.remote.api.AgnesApi
import com.leohu.expense.data.remote.api.WebhookApi
import com.leohu.expense.data.remote.dto.*
import com.leohu.expense.domain.model.*
import com.leohu.expense.domain.repository.ExpenseRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ExpenseRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase,
    private val api: AgnesApi,
    private val webhookApi: WebhookApi
) : ExpenseRepository {

    private val sourceImageDao = db.sourceImageDao()
    private val paymentRecordDao = db.paymentRecordDao()
    private val creditCardDao = db.creditCardDao()
    private val eWalletAccountDao = db.eWalletAccountDao()
    private val gson = Gson()

    override suspend fun enqueueSourceImage(localPath: String): String = withContext(Dispatchers.IO) {
        val finalPath = if (localPath.startsWith("content://") || localPath.startsWith("file://")) {
            saveUriToInternalStorage(Uri.parse(localPath))
        } else {
            localPath
        }

        val id = UUID.randomUUID().toString()
        val entity = SourceImage(
            id = id,
            localPath = finalPath,
            createdAt = System.currentTimeMillis(),
            status = SourceImageStatus.PENDING_OCR
        ).toEntity()
        sourceImageDao.insert(entity)
        id
    }

    private fun saveUriToInternalStorage(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw Exception("無法讀取圖片來源")
        
        val outputDir = File(context.filesDir, "pending_receipts").apply { mkdirs() }
        val outputFile = File(outputDir, "TEMP_${UUID.randomUUID()}.jpg")
        
        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return outputFile.absolutePath
    }

    override fun getSourceImagesByStatus(status: SourceImageStatus): Flow<List<SourceImage>> {
        return sourceImageDao.getByStatus(status).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSourceImageById(id: String): SourceImage? {
        return sourceImageDao.getById(id)?.toDomain()
    }

    override suspend fun updateSourceImage(image: SourceImage) {
        sourceImageDao.update(image.toEntity())
    }

    override fun getPaymentRecordsByStatus(status: PaymentStatus): Flow<List<PaymentRecord>> {
        return paymentRecordDao.getByStatus(status).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPaymentRecordById(id: String): PaymentRecord? {
        return paymentRecordDao.getById(id)?.toDomain()
    }

    override fun getPaymentRecordByIdFlow(id: String): Flow<PaymentRecord?> {
        return paymentRecordDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun createPaymentRecords(imageId: String, transactions: List<PaymentRecord>) {
        paymentRecordDao.insertAll(transactions.map { it.toEntity() })
    }

    override suspend fun approvePaymentRecord(recordId: String) {
        // Handled in UseCase
    }

    override suspend fun updatePaymentRecord(record: PaymentRecord) {
        paymentRecordDao.update(record.toEntity())
    }

    override suspend fun deletePaymentRecord(record: PaymentRecord) {
        paymentRecordDao.delete(record.toEntity())
    }

    override suspend fun deleteOldApprovedRecords(days: Int) {
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        paymentRecordDao.deleteOldApproved(threshold)
    }

    override suspend fun sendToWebhook(record: PaymentRecord) {
        val params = mutableMapOf(
            "method" to record.method,
            "amount" to record.amount.toString(),
            "description" to (record.description ?: ""),
            "date" to (record.consumeDate ?: ""),
            "currency" to (record.currency ?: "TWD")
        )
        record.account?.let { params["account"] = it }
        record.cardLast4?.let { params["card_last4"] = it }
        
        webhookApi.sendExpense(params)
    }

    override fun getCreditCards(): Flow<List<CreditCard>> {
        return creditCardDao.getAllActive().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addCreditCard(card: CreditCard) {
        creditCardDao.insert(card.toEntity())
    }

    override suspend fun deleteCreditCard(card: CreditCard) {
        creditCardDao.delete(card.toEntity())
    }

    override fun getEWalletAccounts(): Flow<List<EWalletAccount>> {
        return eWalletAccountDao.getAllActive().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllEWalletAccounts(): List<EWalletAccount> {
        return eWalletAccountDao.getAll().map { it.toDomain() }
    }

    override suspend fun addEWalletAccount(account: EWalletAccount) {
        eWalletAccountDao.insert(account.toEntity())
    }

    override suspend fun deleteEWalletAccount(account: EWalletAccount) {
        eWalletAccountDao.delete(account.toEntity())
    }
    
    // Helper method for UploadAndParseWorker to use
    suspend fun parseReceipt(imageId: String, imageFile: File, ewalletAccounts: List<EWalletAccount>): AgnesResponseDto {
        val imageBytes = imageFile.readBytes()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64Image"
        
        val cards = creditCardDao.getAll().map { it.toDomain() }

        val ewalletContext = if (ewalletAccounts.isNotEmpty()) {
            "Known E-Wallet accounts and their keywords for reference:\n" + 
            ewalletAccounts.joinToString("\n") { "- ${it.name}: keywords=[${it.keywords.joinToString()}]" }
        } else ""

        val cardContext = if (cards.isNotEmpty()) {
            "Known Credit Cards for reference:\n" +
            cards.joinToString("\n") { "- ${it.name}: last4 digits=${it.last4 ?: "Unknown"}" }
        } else ""

        val prompt = """
            Analyze the attached receipt image. Extract all transactions. 
            
            $ewalletContext
            
            $cardContext
            
            Strict Instructions:
            1. **Determine 'method' (Priority Rule)**: 
               - Scan the entire image for E-Wallet keywords (e.g., '全點', '全支付', 'Line Points', '街口'). 
               - If any keyword matches a known E-Wallet, set 'method' to that E-Wallet's name.
               - ONLY if no E-Wallet keywords are found, set 'method' to "一般刷卡" (if a card is used) or "現金".
            2. **Identify 'account' (Funding Source)**: 
               - If a credit card was used (either directly or via an E-Wallet), match it against the "Known Credit Cards" list.
               - Use the EXACT card name from the list for 'account'.
               - Extract the last 4 digits for 'card_last4'.
            3. **Data Extraction**: Extract 'amount', 'currency' (default TWD), 'date' (YYYY/MM/DD), and 'description' (merchant name).
            
            Output strictly in JSON format following this schema:
            {
              "schema_version": 1,
              "transactions": [
                {
                  "method": "string",
                  "account": "string or null",
                  "card_last4": "string or null",
                  "amount": number,
                  "currency": "string",
                  "date": "string",
                  "description": "string"
                }
              ],
              "confidence": number
            }
        """.trimIndent()

        val request = AgnesRequest(
            messages = listOf(
                AgnesMessage(
                    role = "user",
                    content = listOf(
                        AgnesContent(type = "text", text = prompt),
                        AgnesContent(type = "image_url", image_url = AgnesImageUrl(url = dataUri))
                    )
                )
            )
        )
        
        val apiKey = "u3IAD98dwEJHBZKZbAeS2lTpyFuo1lk1wV1mQng5R46locxY"
        val response = api.chat("Bearer $apiKey", request)
        val content = response.choices.firstOrNull()?.message?.content ?: throw Exception("Agnes 回傳空內容")
        
        // Clean JSON if LLM includes markdown blocks
        val jsonContent = content.replace("```json", "").replace("```", "").trim()
        
        return try {
            gson.fromJson(jsonContent, AgnesResponseDto::class.java)
        } catch (e: Exception) {
            throw Exception("解析 Agnes 回傳的 JSON 失敗: ${e.message}. Content: $jsonContent")
        }
    }
}
