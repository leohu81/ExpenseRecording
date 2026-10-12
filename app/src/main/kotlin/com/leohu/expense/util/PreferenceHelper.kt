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
        
        // N8n Webhook 設定
        const val KEY_BASE_URL = "n8n_base_url"
        // Agnes API Key 設定
        const val KEY_AGES_API_KEY = "agnes_api_key"
    }
    
    fun getStorageMode(): String = 
        prefs.getString(KEY_STORAGE_MODE, MODE_LOCAL) ?: MODE_LOCAL
    
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
    
    // N8n Webhook Base URL
    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, "") ?: ""
    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }
    
    // Agnes API Key
    fun getApiKey(): String = prefs.getString(KEY_AGES_API_KEY, "") ?: ""
    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_AGES_API_KEY, key).apply()
    }
}
