package com.leohu.expense.ui.home

import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.SourceImage
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.ui.feature.home.HomeViewModel
import com.leohu.expense.util.MockRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {
    
    private lateinit var repository: MockRepository
    private lateinit var viewModel: HomeViewModel
    
    @Before
    fun setup() {
        repository = MockRepository()
        viewModel = HomeViewModel(repository)
    }
    
    @Test
    fun `pending images flow should return empty list initially`() = runTest {
        val pendingImages = repository.getSourceImagesByStatus(SourceImageStatus.PENDING_OCR).first()
        assertEquals(0, pendingImages.size)
    }
    
    @Test
    fun `processing images flow should return empty list initially`() = runTest {
        val processingImages = repository.getSourceImagesByStatus(SourceImageStatus.PROCESSING).first()
        assertEquals(0, processingImages.size)
    }
    
    @Test
    fun `ready images flow should return empty list initially`() = runTest {
        val readyImages = repository.getSourceImagesByStatus(SourceImageStatus.READY).first()
        assertEquals(0, readyImages.size)
    }
    
    @Test
    fun `failed images flow should return empty list initially`() = runTest {
        val failedImages = repository.getSourceImagesByStatus(SourceImageStatus.FAILED).first()
        assertEquals(0, failedImages.size)
    }
    
    @Test
    fun `pending approvals flow should return empty list initially`() = runTest {
        val pendingApprovals = repository.getPaymentRecordsByStatus(PaymentStatus.READY_FOR_APPROVAL).first()
        assertEquals(0, pendingApprovals.size)
    }
}
