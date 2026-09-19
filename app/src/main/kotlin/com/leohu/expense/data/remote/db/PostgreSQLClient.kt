package com.leohu.expense.data.remote.db

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.util.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PostgreSQLClient(private val context: Context) {
    private val gson = Gson()
    private val prefs = PreferenceHelper(context)
    private val tag = "PostgreSQLClient"
    
    // 獲取所有 Profile
    fun getAllProfiles(): List<DbProfile> {
        val json = prefs.getPgProfilesJson() ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DbProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse profiles", e)
            emptyList()
        }
    }
    
    // 保存 Profile 列表
    fun saveProfiles(profiles: List<DbProfile>) {
        val json = gson.toJson(profiles)
        prefs.savePgProfiles(json)
    }
    
    // 獲取當前啟用的 Profile
    fun getCurrentProfile(): DbProfile? {
        val currentId = prefs.getCurrentPgProfileId() ?: return null
        return getAllProfiles().find { it.id == currentId }
    }
    
    // 設置當前使用的 Profile
    fun setCurrentProfile(profileId: String) {
        prefs.setCurrentPgProfileId(profileId)
    }
    
    /**
     * 測試連接 - 使用簡單方式測試 HTTP 連接
     * 不依賴 Retrofit，直接嘗試連接
     */
    suspend fun testConnection(profile: DbProfile): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // 創建 OkHttpClient
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            // 構建 URL
            val url = "http://${profile.host}:${profile.port}/webhook/health"
            Log.d(tag, "Testing connection to: $url")
            
            // 發送請求
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(tag, "Response code: ${response.code}, body: $responseBody")
            
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(tag, "Connection test failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * 插入 PaymentRecord（核准時）
     */
    suspend fun insertPaymentRecord(record: PaymentRecord): Boolean = withContext(Dispatchers.IO) {
        val profile = getCurrentProfile() ?: return@withContext false
        
        return@withContext try {
            val api = createApi(profile)
            val params = buildInsertParams(record)
            api.insertExpense(params)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert payment record: ${e.message}", e)
            false
        }
    }
    
    /**
     * 讀取所有記錄
     */
    suspend fun getExpenses(): List<ExpenseItem> = withContext(Dispatchers.IO) {
        val profile = getCurrentProfile() ?: return@withContext emptyList()
        
        return@withContext try {
            val api = createApi(profile)
            api.getExpenses(profile.username)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get expenses: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 更新記錄
     */
    suspend fun updateExpense(recordId: String, updatedRecord: ExpenseDto): Boolean = withContext(Dispatchers.IO) {
        val profile = getCurrentProfile() ?: return@withContext false
        
        return@withContext try {
            val api = createApi(profile)
            api.updateExpense(profile.username, recordId, updatedRecord)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to update expense: ${e.message}", e)
            false
        }
    }
    
    /**
     * 刪除記錄
     */
    suspend fun deleteExpense(recordId: String): Boolean = withContext(Dispatchers.IO) {
        val profile = getCurrentProfile() ?: return@withContext false
        
        return@withContext try {
            val api = createApi(profile)
            api.deleteExpense(profile.username, recordId)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete expense: ${e.message}", e)
            false
        }
    }
    
    // 構建 INSERT 查詢參數
    private fun buildInsertParams(record: PaymentRecord): Map<String, String> {
        return mapOf(
            "method" to record.method,
            "account" to (record.account ?: ""),
            "amount" to record.amount.toString(),
            "amount_twd" to (record.amountTwd ?: record.amount).toString(),
            "currency" to (record.currency ?: "TWD"),
            "card_last4" to (record.cardLast4 ?: ""),
            "date" to (record.consumeDate ?: ""),
            "description" to (record.description ?: ""),
            "local_id" to record.id
        )
    }
    
    // 創建 API 實例
    private fun createApi(profile: DbProfile): N8nApi {
        val baseUrl = "http://${profile.host}:${profile.port}/"
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(N8nApi::class.java)
    }
}
