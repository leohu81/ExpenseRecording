package com.leohu.expense.ui.feature.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.leohu.expense.domain.model.BackupData
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.Tag
import com.leohu.expense.domain.model.SourceImageStatus
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.domain.usecase.CleanupOldApprovedRecordsUseCase
import com.leohu.expense.util.PreferenceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val cleanupUseCase: CleanupOldApprovedRecordsUseCase,
    private val preferenceHelper: PreferenceHelper
) : ViewModel() {

    private val gson = Gson()
    
    private val _enablePreParseEdit = MutableStateFlow(preferenceHelper.getBool(PreferenceHelper.KEY_ENABLE_PRE_PARSE_EDIT, false))
    val enablePreParseEdit: StateFlow<Boolean> = _enablePreParseEdit.asStateFlow()

    val allTags: Flow<List<Tag>> = repository.getAllTags()

    fun cleanupNow(days: Int) {
        viewModelScope.launch {
            cleanupUseCase(days)
        }
    }

    fun exportBackup(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val cards = repository.getCreditCards().first()
                val ewallets = repository.getEWalletAccounts().first()
                val backup = BackupData(cards, ewallets)
                val json = gson.toJson(backup)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun importBackup(context: Context, uri: Uri, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("無法讀取檔案")

                val backup = gson.fromJson(json, BackupData::class.java)
                
                if (backup == null) {
                    onComplete(false, "備份檔案內容為空")
                    return@launch
                }

                backup.creditCards?.forEach { card ->
                    repository.addCreditCard(card)
                }
                
                backup.eWalletAccounts?.forEach { account ->
                    repository.addEWalletAccount(account)
                }
                
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message)
            }
        }
    }

    fun exportToCsv(context: Context, uri: Uri, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val records = repository.getPaymentRecordsByStatus(
                    com.leohu.expense.domain.model.PaymentStatus.APPROVED
                ).first()
                
                val tags = repository.getAllTags().first()
                val tagMap = tags.associateBy { it.id }
                
                val csvBuilder = StringBuilder()
                // Header - 更新為包含約當台幣
                csvBuilder.appendLine("日期,金額,幣別,約當台幣,支付方式,帳戶,末四碼,商家,標籤,時間戳記")
                
                records.forEach { record ->
                    val date = record.consumeDate ?: ""
                    val amount = record.amount
                    val currency = record.currency ?: "TWD"
                    val amountTwd = record.amountTwd ?: amount
                    val method = record.method
                    val account = record.account ?: ""
                    val cardLast4 = record.cardLast4 ?: ""
                    val description = (record.description ?: "").replace(",", ";")
                    val tagNames = record.tags.mapNotNull { tagMap[it]?.name }.joinToString(";")
                    val timestamp = record.approvedAt ?: record.createdAt
                    
                    csvBuilder.appendLine("$date,$amount,$currency,$amountTwd,$method,$account,$cardLast4,$description,$tagNames,$timestamp")
                }
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csvBuilder.toString())
                    }
                }
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message)
            }
        }
    }

    fun setStorageMode(mode: String) {
        preferenceHelper.setStorageMode(mode)
    }

    val storageMode: String get() = preferenceHelper.getStorageMode()

    fun addTag(name: String, color: String) {
        viewModelScope.launch {
            repository.addTag(Tag(id = java.util.UUID.randomUUID().toString(), name = name, color = color))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    fun togglePreParseEdit(enabled: Boolean) {
        preferenceHelper.setBool(PreferenceHelper.KEY_ENABLE_PRE_PARSE_EDIT, enabled)
        _enablePreParseEdit.value = enabled
    }

    fun simulateParsingFailure() {
        viewModelScope.launch {
            val allImages = repository.getAllSourceImages().first()
            val image = allImages.firstOrNull { it.status != SourceImageStatus.FAILED }
            
            if (image != null) {
                repository.updateSourceImage(
                    image.copy(
                        status = SourceImageStatus.FAILED,
                        lastError = "模擬解析失敗：由使用者手動觸發"
                    )
                )
            }
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val cleanupUseCase: CleanupOldApprovedRecordsUseCase,
        private val preferenceHelper: PreferenceHelper
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, cleanupUseCase, preferenceHelper) as T
        }
    }
}
