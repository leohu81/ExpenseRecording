package com.leohu.expense.data.remote.db

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * n8n Webhook API 介面
 * 
 * n8n webhook URL 格式：http://host:port/webhook/{webhook-id}
 * 
 * 需要在 n8n 建立以下 Webhook：
 * 1. GET  /webhook/health                 - 健康檢查
 * 2. GET  /webhook/expense-list           - 讀取所有記錄  
 * 3. GET  /webhook/expense-insert         - 新增記錄（原有 webhook）
 * 4. PUT  /webhook/expense-update         - 修改記錄
 * 5. DELETE /webhook/expense-delete       - 刪除記錄
 */
interface N8nApi {
    
    // 健康檢查
    @GET("webhook/health")
    suspend fun healthCheck(): ResponseBody
    
    // 讀取所有記錄
    @GET("webhook/expense-list")
    suspend fun getExpenses(@Header("X-API-Key") apiKey: String): List<ExpenseItem>
    
    // 修改記錄
    @PUT("webhook/expense-update")
    suspend fun updateExpense(
        @Header("X-API-Key") apiKey: String,
        @Query("id") id: String,
        @Body expense: ExpenseDto
    ): ExpenseItem
    
    // 刪除記錄
    @DELETE("webhook/expense-delete")
    suspend fun deleteExpense(
        @Header("X-API-Key") apiKey: String,
        @Query("id") id: String
    ): Boolean
    
    // 新增記錄（使用原有 webhook）
    @GET("webhook/expense-insert")
    suspend fun insertExpense(@QueryMap params: Map<String, String>)
}

data class ExpenseItem(
    val id: Int? = null,
    val method: String? = null,
    val account: String? = null,
    val amount: Double? = null,
    val amount_twd: Double? = null,
    val currency: String? = null,
    val card_last4: String? = null,
    val consume_date: String? = null,
    val description: String? = null,
    val status: String? = null,
    val created_at: Long? = null,
    val approved_at: Long? = null
)

data class ExpenseDto(
    val method: String,
    val account: String?,
    val amount: Double,
    val amount_twd: Double?,
    val currency: String?,
    val card_last4: String?,
    val consume_date: String?,
    val description: String?,
    val status: String,
    val created_at: Long? = null,
    val approved_at: Long? = null
)
