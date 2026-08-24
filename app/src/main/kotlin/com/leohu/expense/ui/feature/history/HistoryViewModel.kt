package com.leohu.expense.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ImageHistoryItem(
    val sourceImage: SourceImage,
    val paymentRecords: List<PaymentRecord> = emptyList()
)

class HistoryViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allImages: StateFlow<List<SourceImage>> = repository
        .getAllSourceImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyItems: StateFlow<List<ImageHistoryItem>> = combine(
        allImages,
        repository.getPaymentRecordsByStatus(PaymentStatus.READY_FOR_APPROVAL),
        repository.getPaymentRecordsByStatus(PaymentStatus.APPROVED)
    ) { images, readyRecords, approvedRecords ->
        val allRecords = readyRecords + approvedRecords
        images.map { image ->
            // 不管圖片狀態，只要有關聯的 record 都抓出來顯示
            val records = allRecords.filter { it.sourceImageId == image.id }
            ImageHistoryItem(sourceImage = image, paymentRecords = records)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
