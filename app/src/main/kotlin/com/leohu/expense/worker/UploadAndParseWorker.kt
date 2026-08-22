package com.leohu.expense.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.leohu.expense.app.ExpenseApplication
import com.leohu.expense.data.repository.ExpenseRepositoryImpl
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImageStatus
import retrofit2.HttpException
import java.io.File
import java.util.*

class UploadAndParseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val imageId = inputData.getString("image_id") ?: return Result.failure()
        val repository = (applicationContext as ExpenseApplication).repository as ExpenseRepositoryImpl
        
        val sourceImage = repository.getSourceImageById(imageId) ?: return Result.failure()
        if (sourceImage.status == SourceImageStatus.READY) return Result.success()

        return try {
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.PROCESSING,
                lastError = if (runAttemptCount > 0) "正在重試 (第 $runAttemptCount 次)..." else null
            ))
            
            val imageFile = File(sourceImage.localPath)
            if (!imageFile.exists()) {
                repository.updateSourceImage(sourceImage.copy(
                    status = SourceImageStatus.FAILED,
                    lastError = "圖片檔案不存在"
                ))
                return Result.failure()
            }

            val ewalletAccounts = repository.getAllEWalletAccounts()
            
            val response = repository.parseReceipt(imageId, imageFile, ewalletAccounts)
            
            val paymentRecords = response.transactions.map { dto ->
                PaymentRecord(
                    id = UUID.randomUUID().toString(),
                    sourceImageId = imageId,
                    method = dto.method,
                    account = dto.account,
                    cardLast4 = dto.card_last4,
                    amount = dto.amount,
                    currency = dto.currency,
                    consumeDate = dto.date,
                    description = dto.description,
                    status = PaymentStatus.READY_FOR_APPROVAL,
                    createdAt = System.currentTimeMillis()
                )
            }
            
            repository.createPaymentRecords(imageId, paymentRecords)
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.READY,
                lastError = null
            ))

            Result.success()
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = if (!errorBody.isNullOrBlank()) {
                "伺服器錯誤: $errorBody"
            } else {
                "HTTP 錯誤: ${e.code()} ${e.message()}"
            }
            
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.FAILED,
                lastError = errorMessage,
                retryCount = sourceImage.retryCount + 1
            ))
            Result.failure() // HTTP 錯誤通常不需要立即重試，除非是 503 等
        } catch (e: Exception) {
            e.printStackTrace()
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.FAILED,
                lastError = "解析失敗: ${e.message ?: "未知錯誤"}",
                retryCount = sourceImage.retryCount + 1
            ))
            Result.retry()
        }
    }
}
