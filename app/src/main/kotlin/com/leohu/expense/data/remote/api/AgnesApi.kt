package com.leohu.expense.data.remote.api

import com.leohu.expense.data.remote.dto.AgnesRequest
import com.leohu.expense.data.remote.dto.AgnesResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AgnesApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body request: AgnesRequest
    ): AgnesResponse
}
