
package com.leohu.expense.share

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.leohu.expense.app.ExpenseApplication
import com.leohu.expense.domain.usecase.EnqueueSourceImageUseCase
import com.leohu.expense.worker.ImageCompressWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareReceiveActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (type?.startsWith("image/") == true) {
            val uris = when (action) {
                Intent.ACTION_SEND -> {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) } ?: emptyList()
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
                }
                else -> emptyList()
            }

            if (uris.isNotEmpty()) {
                handleUris(uris)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun handleUris(uris: List<Uri>) {
        val app = application as com.leohu.expense.app.ExpenseApplication
        val repository = app.repository
        val preferenceHelper = app.preferenceHelper
        val enqueueUseCase = com.leohu.expense.domain.usecase.EnqueueSourceImageUseCase(repository)
        
        CoroutineScope(Dispatchers.Main).launch {
            val imageIds = mutableListOf<String>()
            
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        imageIds.add(enqueueUseCase(uri.toString()))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            if (imageIds.isNotEmpty()) {
                val shouldEdit = preferenceHelper.getBool(com.leohu.expense.util.PreferenceHelper.KEY_ENABLE_PRE_PARSE_EDIT, false)
                
                if (shouldEdit) {
                    // 跳轉到主畫面並開啟編輯
                    val mainIntent = Intent(this@ShareReceiveActivity, com.leohu.expense.app.MainActivity::class.java).apply {
                        putExtra("image_ids", imageIds.toTypedArray())
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(mainIntent)
                } else {
                    // 直接啟動背景解析
                    imageIds.forEach { imageId ->
                        val compressRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
                            .setInputData(workDataOf("image_id" to imageId))
                            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()
                        WorkManager.getInstance(applicationContext).enqueue(compressRequest)
                    }
                }
            }
            finish()
        }
    }
}
