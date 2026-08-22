package com.leohu.expense.ui.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.leohu.expense.domain.model.BackupData
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.domain.usecase.CleanupOldApprovedRecordsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val cleanupUseCase: CleanupOldApprovedRecordsUseCase
) : ViewModel() {

    private val gson = Gson()

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
                
                // Import Credit Cards
                backup.creditCards.forEach { card ->
                    repository.addCreditCard(card)
                }
                
                // Import E-Wallets
                backup.eWalletAccounts.forEach { account ->
                    repository.addEWalletAccount(account)
                }
                
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message)
            }
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val cleanupUseCase: CleanupOldApprovedRecordsUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, cleanupUseCase) as T
        }
    }
}
