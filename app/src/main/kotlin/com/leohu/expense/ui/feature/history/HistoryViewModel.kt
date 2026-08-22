package com.leohu.expense.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val approvedRecords: StateFlow<List<PaymentRecord>> = repository
        .getPaymentRecordsByStatus(PaymentStatus.APPROVED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
