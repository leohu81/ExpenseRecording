package com.leohu.expense.ui.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.CreditCard
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CardViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val cards: StateFlow<List<CreditCard>> = repository
        .getCreditCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCard(name: String, last4: String?) {
        viewModelScope.launch {
            val card = CreditCard(
                id = UUID.randomUUID().toString(),
                name = name,
                last4 = last4,
                issuer = null
            )
            repository.addCreditCard(card)
        }
    }

    fun updateCard(card: CreditCard) {
        viewModelScope.launch {
            repository.addCreditCard(card) // insert handles update in Room
        }
    }

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
        }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CardViewModel(repository) as T
        }
    }
}
