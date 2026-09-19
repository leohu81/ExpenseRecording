package com.leohu.expense.util

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PreferenceHelper(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val KEY_STORAGE_MODE = "storage_mode"
        const val MODE_CLOUD = "cloud"
        const val MODE_LOCAL = "local"
        const val KEY_ENABLE_PRE_PARSE_EDIT = "enable_pre_parse_edit"
        
        // PostgreSQL Profile 相關
        const val KEY_PG_PROFILES = "pg_profiles"
        const val KEY_CURRENT_PG_PROFILE_ID = "current_pg_profile_id"
    }
    
    fun getStorageMode(): String = 
        prefs.getString(KEY_STORAGE_MODE, MODE_CLOUD) ?: MODE_CLOUD
    
    fun setStorageMode(mode: String) {
        prefs.edit().putString(KEY_STORAGE_MODE, mode).apply()
    }

    fun getBool(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    fun setBool(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolFlow(key: String, default: Boolean): Flow<Boolean> = flow {
        emit(getBool(key, default))
    }
    
    // PostgreSQL 相關方法
    fun savePgProfiles(json: String) {
        prefs.edit().putString(KEY_PG_PROFILES, json).apply()
    }
    
    fun getPgProfilesJson(): String? = prefs.getString(KEY_PG_PROFILES, null)
    
    fun setCurrentPgProfileId(profileId: String) {
        prefs.edit().putString(KEY_CURRENT_PG_PROFILE_ID, profileId).apply()
    }
    
    fun getCurrentPgProfileId(): String? = prefs.getString(KEY_CURRENT_PG_PROFILE_ID, null)
}
