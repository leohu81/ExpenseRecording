package com.leohu.expense.ui.feature.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.data.remote.dto.AgnesResponseDto
import com.leohu.expense.data.remote.dto.AgnesTransactionDto
import com.leohu.expense.data.repository.ExpenseRepositoryImpl
import com.leohu.expense.domain.model.CreditCard
import com.leohu.expense.domain.model.EWalletAccount
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class VoiceInputUiState(
    val recognizedText: String = "",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

class VoiceInputViewModel(
    private val repository: ExpenseRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceInputUiState())
    val uiState: StateFlow<VoiceInputUiState> = _uiState.asStateFlow()

    fun appendText(text: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            recognizedText = if (current.recognizedText.isEmpty()) text
            else "${current.recognizedText} $text"
        )
    }

    fun replaceText(text: String) {
        _uiState.value = _uiState.value.copy(recognizedText = text)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun sendToLLM() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)

            try {
                // 1. 建立 SourceImage (語音輸入用特殊標記)
                val voiceId = UUID.randomUUID().toString()
                val voiceSourceImage = SourceImage(
                    id = voiceId,
                    localPath = "voice_input://$voiceId",
                    createdAt = System.currentTimeMillis(),
                    status = SourceImageStatus.PENDING_OCR
                )
                repository.updateSourceImage(voiceSourceImage)

                // 2. 取得電子支付和信用卡資訊
                val ewallets = repository.getAllEWalletAccounts()
                val cards = mutableListOf<CreditCard>()
                repository.getCreditCards().collect { cardList ->
                    cards.addAll(cardList)
                }

                // 3. 呼叫 LLM 解析
                val response = (repository as? ExpenseRepositoryImpl)
                    ?.parseVoiceInput(
                        voiceText = _uiState.value.recognizedText,
                        ewalletAccounts = ewallets,
                        creditCards = cards
                    ) ?: throw Exception("無法解析語音內容")

                // 4. 轉換為 PaymentRecord
                val paymentRecords = response.transactions.map { dto: AgnesTransactionDto ->
                    PaymentRecord(
                        id = UUID.randomUUID().toString(),
                        sourceImageId = voiceId,
                        method = dto.method,
                        account = dto.account,
                        cardLast4 = dto.card_last4,
                        amount = dto.amount,
                        amountTwd = dto.amount_twd,
                        currency = dto.currency,
                        consumeDate = dto.date,
                        description = dto.description,
                        status = PaymentStatus.READY_FOR_APPROVAL,
                        tags = emptyList(),
                        createdAt = System.currentTimeMillis()
                    )
                }

                // 5. 儲存記錄
                repository.createPaymentRecords(voiceId, paymentRecords)

                _uiState.value = _uiState.value.copy(isProcessing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message ?: "解析失敗"
                )
            }
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VoiceInputViewModel(repository, context) as T
        }
    }
}
