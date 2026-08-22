package com.leohu.expense.ui.feature.ewallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.EWalletAccount
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class EWalletViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val ewallets: StateFlow<List<EWalletAccount>> = repository
        .getEWalletAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEWallet(name: String, keywordsStr: String) {
        viewModelScope.launch {
            val keywords = keywordsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val account = EWalletAccount(
                id = UUID.randomUUID().toString(),
                name = name,
                keywords = keywords
            )
            repository.addEWalletAccount(account)
        }
    }

    fun updateEWallet(account: EWalletAccount, keywordsStr: String) {
        viewModelScope.launch {
            val keywords = keywordsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            repository.addEWalletAccount(account.copy(keywords = keywords))
        }
    }

    fun deleteEWallet(account: EWalletAccount) {
        viewModelScope.launch {
            repository.deleteEWalletAccount(account)
        }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EWalletViewModel(repository) as T
        }
    }
}
