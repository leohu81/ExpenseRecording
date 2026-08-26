package com.leohu.expense.ui.feature.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.*
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.domain.usecase.ApprovePaymentRecordUseCase
import com.leohu.expense.util.PreferenceHelper
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
    private val preferenceHelper: PreferenceHelper,
    private val recordId: String
) : ViewModel() {

    private val _isApproved = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

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
        
        val image = record?.sourceImageId?.let { if (it.isEmpty()) null else repository.getSourceImageById(it) }
        val tagMap = tags.associateBy { it.id }
        
        ApprovalDetailUiState(
            record = record,
            image = image,
            cards = cards,
            ewallets = ewallets,
            tags = tags,
            tagMap = tagMap,
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
            // 自動計算約當台幣同步邏輯
            val finalRecord = if (updatedRecord.currency?.uppercase() == "TWD") {
                updatedRecord.copy(amountTwd = updatedRecord.amount)
            } else {
                updatedRecord
            }
            repository.updatePaymentRecord(finalRecord)
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

    fun deleteRecord() {
        viewModelScope.launch {
            try {
                val record = uiState.value.record ?: return@launch
                repository.deletePaymentRecord(record)
                _isApproved.value = true // 觸發返回
            } catch (e: Exception) {
                _errorMessage.value = "刪除失敗: ${e.message}"
            }
        }
    }

    fun approve() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val record = uiState.value.record ?: return@launch
                
                // 如果原本不是已核准狀態，才需要執行 approveUseCase (處理狀態與 Webhook)
                if (record.status != PaymentStatus.APPROVED) {
                    approveUseCase(recordId)
                } else {
                    // 只是修改已核准的內容，直接儲存即可 (updateRecord 已處理)
                }
                
                _isApproved.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "儲存失敗: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val approveUseCase: ApprovePaymentRecordUseCase,
        private val preferenceHelper: PreferenceHelper,
        private val recordId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApprovalDetailViewModel(repository, approveUseCase, preferenceHelper, recordId) as T
        }
    }
}
