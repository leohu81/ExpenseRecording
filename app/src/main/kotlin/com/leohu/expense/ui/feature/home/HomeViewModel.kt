package com.leohu.expense.ui.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.worker.ImageCompressWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val pendingImageCount: Int = 0,
    val processingImageCount: Int = 0,
    val readyImageCount: Int = 0,
    val failedImageCount: Int = 0,
    val failedImages: List<SourceImage> = emptyList(),
    val pendingApprovalCount: Int = 0
)

class HomeViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getSourceImagesByStatus(SourceImageStatus.PENDING_OCR),
        repository.getSourceImagesByStatus(SourceImageStatus.PROCESSING),
        repository.getSourceImagesByStatus(SourceImageStatus.READY),
        repository.getSourceImagesByStatus(SourceImageStatus.FAILED),
        repository.getPaymentRecordsByStatus(PaymentStatus.READY_FOR_APPROVAL)
    ) { pending, processing, ready, failed, pendingApprovals ->
        HomeUiState(
            pendingImageCount = pending.size,
            processingImageCount = processing.size,
            readyImageCount = ready.size,
            failedImageCount = failed.size,
            failedImages = failed,
            pendingApprovalCount = pendingApprovals.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            val imageId = repository.enqueueSourceImage(uri.toString())
            
            val compressRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
                .setInputData(workDataOf("image_id" to imageId))
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager.getInstance(context).enqueue(compressRequest)
        }
    }
    
    /**
     * 新增：處理多圖選取並根據是否需要編輯決定後續動作
     */
    fun onImagesSelected(
        context: Context, 
        uris: List<Uri>, 
        shouldEdit: Boolean, 
        onNavigateToEdit: (List<String>) -> Unit
    ) {
        viewModelScope.launch {
            val imageIds = uris.map { uri ->
                repository.enqueueSourceImage(uri.toString())
            }
            
            if (shouldEdit) {
                // 傳遞乾淨的 UUID 列表給導航
                onNavigateToEdit(imageIds)
            } else {
                // 直接啟動解析流程
                imageIds.forEach { imageId ->
                    val compressRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
                        .setInputData(workDataOf("image_id" to imageId))
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    WorkManager.getInstance(context).enqueue(compressRequest)
                }
            }
        }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
