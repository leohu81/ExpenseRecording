package com.leohu.expense.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leohu.expense.data.remote.db.DbProfile
import com.leohu.expense.data.remote.db.PostgreSQLClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DatabaseSettingsUiState(
    val profiles: List<DbProfile> = emptyList(),
    val currentProfileId: String? = null,
    val testResult: String? = null,
    val isLoading: Boolean = false
)

class DatabaseSettingsViewModel(
    private val pgClient: PostgreSQLClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DatabaseSettingsUiState())
    val uiState: StateFlow<DatabaseSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadProfiles()
    }
    
    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profiles = pgClient.getAllProfiles()
            val currentId = pgClient.getCurrentProfile()?.id
            _uiState.value = _uiState.value.copy(
                profiles = profiles,
                currentProfileId = currentId,
                isLoading = false
            )
        }
    }
    
    fun addProfile(profile: DbProfile) {
        val profiles = _uiState.value.profiles.toMutableList()
        profiles.add(profile)
        pgClient.saveProfiles(profiles)
        pgClient.setCurrentProfile(profile.id)
        _uiState.value = _uiState.value.copy(
            profiles = profiles,
            currentProfileId = profile.id
        )
    }
    
    fun updateProfile(profile: DbProfile) {
        val profiles = _uiState.value.profiles.map {
            if (it.id == profile.id) profile else it
        }
        pgClient.saveProfiles(profiles)
        _uiState.value = _uiState.value.copy(profiles = profiles)
    }
    
    fun deleteProfile(profileId: String) {
        val profiles = _uiState.value.profiles.filter { it.id != profileId }
        pgClient.saveProfiles(profiles)
        val newCurrentId = profiles.firstOrNull()?.id
        pgClient.setCurrentProfile(newCurrentId ?: "")
        _uiState.value = _uiState.value.copy(
            profiles = profiles,
            currentProfileId = newCurrentId
        )
    }
    
    fun selectProfile(profileId: String) {
        pgClient.setCurrentProfile(profileId)
        _uiState.value = _uiState.value.copy(currentProfileId = profileId)
    }
    
    fun testConnection(profile: DbProfile, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = pgClient.testConnection(profile)
            _uiState.value = _uiState.value.copy(testResult = if (success) "連接成功" else "連接失敗")
            callback(success)
        }
    }
    
    fun testCurrentProfile(callback: (Boolean) -> Unit) {
        val current = pgClient.getCurrentProfile() ?: return
        testConnection(current, callback)
    }
    
    class Factory(
        private val pgClient: PostgreSQLClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DatabaseSettingsViewModel(pgClient) as T
        }
    }
}
