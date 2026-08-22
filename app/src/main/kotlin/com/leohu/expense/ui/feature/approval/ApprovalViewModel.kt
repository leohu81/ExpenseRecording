package com.leohu.expense.ui.feature.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.domain.usecase.ApprovePaymentRecordUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ApprovalViewModel(
    private val repository: ExpenseRepository,
    private val approveUseCase: ApprovePaymentRecordUseCase
) : ViewModel() {

    val pendingApprovals: StateFlow<List<PaymentRecord>> = repository
        .getPaymentRecordsByStatus(PaymentStatus.READY_FOR_APPROVAL)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun batchApprove() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                ids.forEach { id ->
                    approveUseCase(id)
                }
                clearSelection()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "批次核准失敗: ${e.message}"
            }
        }
    }

    fun batchDelete() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                ids.forEach { id ->
                    repository.getPaymentRecordById(id)?.let { record ->
                        repository.deletePaymentRecord(record)
                    }
                }
                clearSelection()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "批次刪除失敗: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val approveUseCase: ApprovePaymentRecordUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApprovalViewModel(repository, approveUseCase) as T
        }
    }
}
