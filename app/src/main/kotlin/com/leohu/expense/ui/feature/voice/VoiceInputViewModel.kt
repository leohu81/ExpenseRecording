package com.leohu.expense.ui.feature.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    fun enqueueVoiceParsing(context: Context) {
        val text = _uiState.value.recognizedText
        if (text.isBlank()) return
        
        viewModelScope.launch {
            try {
                // 1. 建立 SourceImage (語音輸入用特殊標記，將文字存入 preDescription)
                val voiceId = repository.enqueueSourceImage("voice_input://$text")
                // 更新 preDescription
                val voiceSourceImage = SourceImage(
                    id = voiceId,
                    localPath = "voice_input://$voiceId",
                    createdAt = System.currentTimeMillis(),
                    status = SourceImageStatus.PENDING_OCR,
                    preDescription = text
                )
                repository.updateSourceImage(voiceSourceImage)

                // 2. 啟動背景 Worker 解析
                val parseRequest = androidx.work.OneTimeWorkRequestBuilder<com.leohu.expense.worker.UploadAndParseWorker>()
                    .setInputData(androidx.work.workDataOf("image_id" to voiceId))
                    .build()
                
                androidx.work.WorkManager.getInstance(context).enqueue(parseRequest)
            } catch (e: Exception) {
                // 靜默處理錯誤
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
