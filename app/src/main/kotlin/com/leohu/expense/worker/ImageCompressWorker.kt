package com.leohu.expense.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.*
import com.leohu.expense.app.ExpenseApplication
import com.leohu.expense.domain.model.SourceImageStatus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class ImageCompressWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val imageId = inputData.getString("image_id") ?: return Result.failure()
        val repository = (applicationContext as ExpenseApplication).repository
        
        val sourceImage = repository.getSourceImageById(imageId) ?: return Result.failure()
        
        return try {
            val inputStream: InputStream = if (sourceImage.localPath.startsWith("content://")) {
                applicationContext.contentResolver.openInputStream(Uri.parse(sourceImage.localPath))
            } else {
                FileInputStream(File(sourceImage.localPath))
            } ?: throw Exception("無法讀取原始圖片檔案")

            val bitmap = inputStream.use {
                BitmapFactory.decodeStream(it)
            } ?: throw Exception("無法解碼圖片內容")

            val scaledBitmap = scaleBitmap(bitmap, 1080)
            
            val outputDir = File(applicationContext.filesDir, "receipts").apply { mkdirs() }
            val outputFile = File(outputDir, "${UUID.randomUUID()}.jpg")
            
            FileOutputStream(outputFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            repository.updateSourceImage(sourceImage.copy(
                localPath = outputFile.absolutePath
            ))

            // 壓縮完成後，如果是暫存檔就刪除
            if (!sourceImage.localPath.startsWith("content://")) {
                val oldFile = File(sourceImage.localPath)
                if (oldFile.exists() && oldFile.parentFile?.name == "pending_receipts") {
                    oldFile.delete()
                }
            }

            val uploadRequest = OneTimeWorkRequestBuilder<UploadAndParseWorker>()
                .setInputData(workDataOf("image_id" to imageId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            
            WorkManager.getInstance(applicationContext).enqueue(uploadRequest)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            repository.updateSourceImage(sourceImage.copy(
                status = SourceImageStatus.FAILED,
                lastError = "圖片壓縮失敗: ${e.message ?: "未知錯誤"}"
            ))
            Result.failure()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()
        
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxEdge
            newHeight = (maxEdge / ratio).toInt()
        } else {
            newHeight = maxEdge
            newWidth = (maxEdge * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
