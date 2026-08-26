package com.leohu.expense.ui.feature.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.*
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.util.PreferenceHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ManualEntryUiState(
    val description: String = "",
    val amount: String = "",
    val amountTwd: String = "",
    val currency: String = "TWD",
    val consumeDate: String = "",
    val method: String = "一般刷卡",
    val account: String? = null,
    val cardLast4: String? = null,
    val selectedTags: List<String> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    val ewallets: List<EWalletAccount> = emptyList(),
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class ManualEntryViewModel(
    private val repository: ExpenseRepository,
    private val preferenceHelper: PreferenceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualEntryUiState())
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    init {
        val today = java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(consumeDate = today) }

        viewModelScope.launch {
            combine(
                repository.getAllTags().onStart { emit(emptyList()) },
                repository.getCreditCards().onStart { emit(emptyList()) },
                repository.getEWalletAccounts().onStart { emit(emptyList()) }
            ) { tags, cards, ewallets ->
                _uiState.update { it.copy(allTags = tags, cards = cards, ewallets = ewallets) }
            }.collect()
        }
    }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    
    fun onAmountChange(value: String) {
        _uiState.update { state ->
            val newState = state.copy(amount = value)
            // 如果是台幣，自動同步約當台幣
            if (state.currency.uppercase() == "TWD") {
                newState.copy(amountTwd = value)
            } else {
                newState
            }
        }
    }

    fun onAmountTwdChange(value: String) = _uiState.update { it.copy(amountTwd = value) }

    fun onCurrencyChange(value: String) {
        _uiState.update { state ->
            val newState = state.copy(currency = value)
            if (value.uppercase() == "TWD") {
                newState.copy(amountTwd = state.amount)
            } else {
                newState
            }
        }
    }

    fun onDateChange(value: String) = _uiState.update { it.copy(consumeDate = value) }
    fun onMethodChange(value: String) = _uiState.update { it.copy(method = value) }
    fun onAccountChange(card: CreditCard?) = _uiState.update { 
        it.copy(account = card?.name, cardLast4 = card?.last4) 
    }

    fun toggleTag(tagId: String) {
        _uiState.update { state ->
            val newTags = if (state.selectedTags.contains(tagId)) {
                state.selectedTags - tagId
            } else {
                state.selectedTags + tagId
            }
            state.copy(selectedTags = newTags)
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val amountDouble = state.amount.toDoubleOrNull()
            if (amountDouble == null) {
                _uiState.update { it.copy(errorMessage = "金額格式錯誤") }
                return@launch
            }
            
            val amountTwdDouble = state.amountTwd.toDoubleOrNull() ?: amountDouble

            try {
                val record = PaymentRecord(
                    id = UUID.randomUUID().toString(),
                    sourceImageId = "", 
                    method = state.method,
                    account = state.account,
                    cardLast4 = state.cardLast4,
                    amount = amountDouble,
                    amountTwd = amountTwdDouble,
                    currency = state.currency,
                    consumeDate = state.consumeDate,
                    description = state.description,
                    status = PaymentStatus.APPROVED, 
                    tags = state.selectedTags,
                    createdAt = System.currentTimeMillis(),
                    approvedAt = System.currentTimeMillis()
                )

                repository.updatePaymentRecord(record)
                
                if (preferenceHelper.getStorageMode() == PreferenceHelper.MODE_CLOUD) {
                    repository.sendToWebhook(record)
                }

                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "儲存失敗: ${e.message}") }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    class Factory(
        private val repository: ExpenseRepository,
        private val preferenceHelper: PreferenceHelper
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManualEntryViewModel(repository, preferenceHelper) as T
        }
    }
}
