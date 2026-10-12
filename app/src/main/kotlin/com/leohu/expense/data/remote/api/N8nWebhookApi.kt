package com.leohu.expense.data.remote.api

import com.leohu.expense.data.remote.dto.ExpenseDto
import com.leohu.expense.domain.model.PaymentRecord
import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * N8n Webhook API 介面
 *
 * 對應的 n8n workflow webhook：
 * - GET  /webhook/health
 * - POST /webhook/expense-insert
 * - PUT  /webhook/expense-update
 * - DELETE /webhook/expense-delete
 */
interface N8nWebhookApi {

    @GET("webhook/health")
    suspend fun healthCheck(): ResponseBody

    @GET("webhook/expense-insert")
    suspend fun insertExpense(
        @QueryMap params: Map<String, String>
    )

    @PUT("webhook/expense-update")
    suspend fun updateExpense(
        @Query("id") id: String,
        @Body expense: ExpenseDto
    )

    @DELETE("webhook/expense-delete")
    suspend fun deleteExpense(
        @Query("id") id: String
    )
}
