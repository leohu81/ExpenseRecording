package com.leohu.expense.ui.feature.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.CreditCard
import com.leohu.expense.domain.model.EWalletAccount
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.domain.usecase.ApprovePaymentRecordUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ApprovalDetailUiState(
    val record: PaymentRecord? = null,
    val image: SourceImage? = null,
    val cards: List<CreditCard> = emptyList(),
    val ewallets: List<EWalletAccount> = emptyList(),
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

    val uiState: StateFlow<ApprovalDetailUiState> = combine(
        repository.getPaymentRecordByIdFlow(recordId),
        repository.getCreditCards(),
        repository.getEWalletAccounts(),
        _isApproved,
        _errorMessage
    ) { record, cards, ewallets, isApproved, error ->
        val image = record?.sourceImageId?.let { repository.getSourceImageById(it) }
        
        ApprovalDetailUiState(
            record = record,
            image = image,
            cards = cards,
            ewallets = ewallets,
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
