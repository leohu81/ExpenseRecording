package com.leohu.expense.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.leohu.expense.app.ExpenseApplication
import com.leohu.expense.data.repository.ExpenseRepositoryImpl
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.util.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
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
                lastError = if (runAttemptCount > 0) "正在解析..." else null
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
            val cards = repository.getCreditCards().firstOrNull() ?: emptyList()
            
            val response = repository.parseReceipt(imageId, imageFile, ewalletAccounts, cards)
            
            val paymentRecords = response.transactions.map { dto ->
                PaymentRecord(
                    id = UUID.randomUUID().toString(),
                    sourceImageId = imageId,
                    method = dto.method,
                    account = dto.account,
                    cardLast4 = dto.card_last4,
                    amount = dto.amount,
                    amountTwd = dto.amount_twd, // 從 AI 結果取得
                    currency = dto.currency,
                    consumeDate = dto.date,
                    description = sourceImage.preDescription ?: dto.description,
                    status = PaymentStatus.READY_FOR_APPROVAL,
                    tags = sourceImage.tags,
                    createdAt = System.currentTimeMillis()
                )
            }
            
            repository.createPaymentRecords(imageId, paymentRecords)
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.READY,
                lastError = null
            ))

            val count = paymentRecords.size
            NotificationHelper.showParseResultNotification(
                applicationContext,
                true,
                "成功解析出 $count 筆消費記錄，點擊前往核准。"
            )

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

            NotificationHelper.showParseResultNotification(
                applicationContext,
                false,
                "解析失敗: $errorMessage"
            )

            Result.failure()
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = e.message ?: "未知錯誤"
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.FAILED,
                lastError = "解析失敗: $errorMsg",
                retryCount = sourceImage.retryCount + 1
            ))

            NotificationHelper.showParseResultNotification(
                applicationContext,
                false,
                "解析失敗: $errorMsg"
            )

            Result.failure()
        }
    }
}
