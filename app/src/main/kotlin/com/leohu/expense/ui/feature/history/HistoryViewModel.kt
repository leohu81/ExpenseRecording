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

/**
 * 歷史記錄顯示項目的密封類
 */
sealed class HistoryUiItem {
    // 獨立的消費記錄（即使多筆記錄屬於同一張圖，也拆開顯示）
    data class Record(val record: PaymentRecord, val sourceImage: SourceImage?) : HistoryUiItem()
    // 只有圖片但還沒有記錄的項目（等待解析、解析中、解析失敗）
    data class ImageOnly(val sourceImage: SourceImage) : HistoryUiItem()

    val id: String get() = when(this) {
        is Record -> record.id
        is ImageOnly -> sourceImage.id
    }

    val createdAt: Long get() = when(this) {
        is Record -> record.createdAt
        is ImageOnly -> sourceImage.createdAt
    }
}

class HistoryViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val allImages = repository.getAllSourceImages()
    private val readyRecords = repository.getPaymentRecordsByStatus(PaymentStatus.READY_FOR_APPROVAL)
    private val approvedRecords = repository.getPaymentRecordsByStatus(PaymentStatus.APPROVED)

    val historyItems: StateFlow<List<HistoryUiItem>> = combine(
        allImages,
        readyRecords,
        approvedRecords
    ) { images, ready, approved ->
        val allRecords = ready + approved
        val items = mutableListOf<HistoryUiItem>()
        
        // 1. 先處理所有的消費記錄
        allRecords.forEach { record ->
            val image = images.find { it.id == record.sourceImageId }
            items.add(HistoryUiItem.Record(record, image))
        }
        
        // 2. 處理還沒有產生記錄的圖片（例如解析中或失敗的）
        images.forEach { image ->
            val hasRecord = allRecords.any { it.sourceImageId == image.id }
            if (!hasRecord) {
                items.add(HistoryUiItem.ImageOnly(image))
            }
        }
        
        // 按時間倒序排列
        items.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
