package com.leohu.expense.ui.feature.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.CreditCard
import com.leohu.expense.domain.model.EWalletAccount
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.model.Tag
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.domain.usecase.ApprovePaymentRecordUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ApprovalDetailUiState(
    val record: PaymentRecord? = null,
    val image: SourceImage? = null,
    val cards: List<CreditCard> = emptyList(),
    val ewallets: List<EWalletAccount> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val tagMap: Map<String, Tag> = emptyMap(),
    val isLoading: Boolean = false,
    val isApproved: Boolean = false,
    val errorMessage: String? = null
)

class ApprovalDetailViewModel(
    private val repository: ExpenseRepository,
    private val approveUseCase: ApprovePaymentRecordUseCase,
    private val recordId: String
) : ViewModel() {

    private val _isApproved = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // 優化後的資料組合邏輯，確保所有資料流都有初始值
    val uiState: StateFlow<ApprovalDetailUiState> = combine(
        repository.getPaymentRecordByIdFlow(recordId).onStart { emit(null) },
        repository.getCreditCards().onStart { emit(emptyList()) },
        repository.getEWalletAccounts().onStart { emit(emptyList()) },
        repository.getAllTags().onStart { emit(emptyList()) },
        _isApproved,
        _errorMessage
    ) { flows ->
        val record = flows[0] as? PaymentRecord?
        val cards = flows[1] as? List<CreditCard> ?: emptyList()
        val ewallets = flows[2] as? List<EWalletAccount> ?: emptyList()
        val tags = flows[3] as? List<Tag> ?: emptyList()
        val isApproved = flows[4] as? Boolean ?: false
        val error = flows[5] as? String
        
        // 注意：這裡如果 record 為 null，會導致 image 也為 null
        val image = record?.sourceImageId?.let { repository.getSourceImageById(it) }
        val tagMap = tags.associateBy { it.id }
        
        ApprovalDetailUiState(
            record = record,
            image = image,
            cards = cards,
            ewallets = ewallets,
            tags = tags,
            tagMap = tagMap,
            // 只有當我們確定是在等待 record 載入時才顯示 Loading
            isLoading = record == null,
            isApproved = isApproved,
            errorMessage = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ApprovalDetailUiState(isLoading = true)
    )

    fun updateRecord(updatedRecord: PaymentRecord) {
        viewModelScope.launch {
            repository.updatePaymentRecord(updatedRecord)
        }
    }

    fun addTagToRecord(tagId: String) {
        viewModelScope.launch {
            val record = uiState.value.record ?: return@launch
            if (!record.tags.contains(tagId)) {
                val updatedRecord = record.copy(tags = record.tags + tagId)
                repository.updatePaymentRecord(updatedRecord)
                repository.addTagToPayment(record.id, tagId)
            }
        }
    }

    fun removeTagFromRecord(tagId: String) {
        viewModelScope.launch {
            val record = uiState.value.record ?: return@launch
            val updatedRecord = record.copy(tags = record.tags - tagId)
            repository.updatePaymentRecord(updatedRecord)
            repository.removeTagFromPayment(record.id, tagId)
        }
    }

    fun approve() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                approveUseCase(recordId)
                _isApproved.value = true
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
        private val approveUseCase: ApprovePaymentRecordUseCase,
        private val recordId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApprovalDetailViewModel(repository, approveUseCase, recordId) as T
        }
    }
}
