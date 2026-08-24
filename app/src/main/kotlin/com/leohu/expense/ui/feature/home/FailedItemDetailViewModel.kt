package com.leohu.expense.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.CreditCard
import com.leohu.expense.domain.model.EWalletAccount
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.worker.ImageCompressWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

data class FailedItemDetailUiState(
    val record: PaymentRecord? = null,
    val sourceImage: SourceImage? = null,
    val cards: List<CreditCard> = emptyList(),
    val ewallets: List<EWalletAccount> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FailedItemDetailViewModel(
    private val repository: ExpenseRepository,
    private val sourceImageId: String
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(FailedItemDetailUiState(isLoading = true))
    val uiState: StateFlow<FailedItemDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sourceImageFlow = flow { emit(repository.getSourceImageById(sourceImageId)) }
            val readyRecordsFlow = repository.getPaymentRecordsByStatus(com.leohu.expense.domain.model.PaymentStatus.READY_FOR_APPROVAL)
            val approvedRecordsFlow = repository.getPaymentRecordsByStatus(com.leohu.expense.domain.model.PaymentStatus.APPROVED)
            val cardsFlow = repository.getCreditCards()
            val ewalletsFlow = repository.getEWalletAccounts()

            combine(
                combine(sourceImageFlow, readyRecordsFlow) { a, b -> a to b },
                combine(approvedRecordsFlow, cardsFlow) { a, b -> a to b },
                combine(ewalletsFlow, _errorMessage) { a, b -> a to b }
            ) { d1, d2, d3 ->
                val sourceImage = d1.first
                val readyRecords = d1.second
                val approvedRecords = d2.first
                val cards = d2.second
                val ewallets = d3.first
                val error = d3.second

                val allRecords = readyRecords + approvedRecords
                val record = allRecords.firstOrNull { it.sourceImageId == sourceImageId }
                
                FailedItemDetailUiState(
                    record = record,
                    sourceImage = sourceImage,
                    cards = cards,
                    ewallets = ewallets,
                    isLoading = sourceImage == null,
                    errorMessage = error
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateRecord(updatedRecord: PaymentRecord) {
        viewModelScope.launch {
            repository.updatePaymentRecord(updatedRecord)
        }
    }

    fun retryParse(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val sourceImage = repository.getSourceImageById(sourceImageId)
                if (sourceImage != null) {
                    repository.updateSourceImage(
                        sourceImage.copy(
                            status = SourceImageStatus.PENDING_OCR,
                            retryCount = 0,
                            lastError = null
                        )
                    )
                    
                    val compressRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
                        .setInputData(workDataOf("image_id" to sourceImageId))
                        .build()
                    WorkManager.getInstance(context).enqueue(compressRequest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "重試失敗: ${e.message}"
            }
        }
    }

    fun approve() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                repository.approvePaymentRecord(sourceImageId)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "核准失敗: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val sourceImageId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FailedItemDetailViewModel(repository, sourceImageId) as T
        }
    }
}
