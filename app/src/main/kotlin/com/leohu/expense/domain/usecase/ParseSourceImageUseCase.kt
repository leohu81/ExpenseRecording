package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository

class ParseSourceImageUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(imageId: String) {
        val image = repository.getSourceImageById(imageId) ?: return
        if (image.status == SourceImageStatus.PENDING_OCR) {
            repository.updateSourceImage(image.copy(status = SourceImageStatus.PROCESSING))
            // Worker is usually responsible for the actual API call
        }
    }
}
