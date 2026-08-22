package com.leohu.expense.data.remote.api

import retrofit2.http.GET
import retrofit2.http.QueryMap

interface WebhookApi {
    @GET("webhook/ExpenseCheck3")
    suspend fun sendExpense(
        @QueryMap params: Map<String, String>
    )
}
