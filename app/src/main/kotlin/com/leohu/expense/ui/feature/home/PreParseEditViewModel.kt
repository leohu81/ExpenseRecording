package com.leohu.expense.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.model.Tag
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.worker.ImageCompressWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

data class SourceImageEditItem(
    val id: String,
    val localPath: String,
    val preDescription: String?,
    val tagIds: List<String> = emptyList()
)

data class PreParseEditUiState(
    val images: List<SourceImageEditItem> = emptyList(),
    val selectedImageId: String? = null,
    val allTags: List<Tag> = emptyList(),
    val batchDescription: String = "",
    val isParsing: Boolean = false,
    val isParsed: Boolean = false
) {
    val selectedImage: SourceImageEditItem? get() = images.find { it.id == selectedImageId }
}

class PreParseEditViewModel(
    private val repository: ExpenseRepository,
    private val imageIds: List<String>
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreParseEditUiState())
    val uiState: StateFlow<PreParseEditUiState> = _uiState

    init {
        viewModelScope.launch {
            val tags = repository.getAllTags().first()
            val images = imageIds.mapNotNull { id ->
                repository.getSourceImageById(id)?.let { image ->
                    SourceImageEditItem(image.id, image.localPath, image.preDescription, image.tags)
                }
            }
            _uiState.update { state -> 
                state.copy(
                    images = images, 
                    allTags = tags,
                    selectedImageId = images.firstOrNull()?.id
                ) 
            }
        }
    }

    fun removeImage(imageId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(images = state.images.filter { it.id != imageId })
            }
        }
    }

    fun updateBatchDescription(description: String) {
        _uiState.update { it.copy(batchDescription = description) }
    }

    fun selectImage(imageId: String) {
        _uiState.update { it.copy(selectedImageId = imageId) }
    }

    fun updateImageDescription(description: String) {
        val selectedImageId = _uiState.value.selectedImageId ?: return
        _uiState.update { state ->
            state.copy(
                images = state.images.map {
                    if (it.id == selectedImageId) it.copy(preDescription = description) else it
                }
            )
        }
    }

    fun toggleTagForImage(imageId: String, tagId: String) {
        _uiState.update { state ->
            state.copy(
                images = state.images.map {
                    if (it.id == imageId) {
                        val newTags = if (it.tagIds.contains(tagId)) {
                            it.tagIds - tagId
                        } else {
                            it.tagIds + tagId
                        }
                        it.copy(tagIds = newTags)
                    } else it
                }
            )
        }
    }

    fun startParsing(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true) }
            
            val batchDesc = _uiState.value.batchDescription
            
            _uiState.value.images.forEach { item ->
                val finalDesc = if (!item.preDescription.isNullOrBlank()) {
                    item.preDescription
                } else if (batchDesc.isNotBlank()) {
                    batchDesc
                } else {
                    null
                }
                
                val sourceImage = repository.getSourceImageById(item.id)
                if (sourceImage != null) {
                    repository.updateSourceImage(
                        sourceImage.copy(
                            preDescription = finalDesc,
                            tags = item.tagIds,
                            status = SourceImageStatus.PENDING_OCR
                        )
                    )
                    
                    val compressRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
                        .setInputData(workDataOf("image_id" to item.id))
                        .build()
                    WorkManager.getInstance(context).enqueue(compressRequest)
                }
            }
            
            _uiState.update { it.copy(isParsing = false, isParsed = true) }
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val imageIds: List<String>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PreParseEditViewModel(repository, imageIds) as T
        }
    }
}
