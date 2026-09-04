
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

class ShareReceiveActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (type?.startsWith("image/") == true) {
            when (action) {
                Intent.ACTION_SEND -> {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                        handleUri(uri)
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                        uris.forEach { handleUri(it) }
                    }
                }
            }
        }
        finish()
    }

    private fun handleUri(uri: Uri) {
        val repository = (application as ExpenseApplication).repository
        val enqueueUseCase = EnqueueSourceImageUseCase(repository)
        
        CoroutineScope(Dispatchers.IO).launch {
            val imageId = enqueueUseCase(uri.toString())
            
            val compressRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
                .setInputData(workDataOf("image_id" to imageId))
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager.getInstance(applicationContext).enqueue(compressRequest)
        }
    }
}
