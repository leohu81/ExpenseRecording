package com.leohu.expense.domain.usecase

import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository

class RetryFailedImageUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(imageId: String) {
        val image = repository.getSourceImageById(imageId) ?: return
        if (image.status == SourceImageStatus.FAILED) {
            repository.updateSourceImage(image.copy(
                status = SourceImageStatus.PENDING_OCR,
                retryCount = 0,
                lastError = null
            ))
            // The worker will pick it up
        }
    }
}
