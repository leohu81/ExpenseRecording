
package com.leohu.expense.app

import android.app.Application
import androidx.room.Room
import com.leohu.expense.data.local.db.AppDatabase
import com.leohu.expense.data.remote.api.AgnesApi
import com.leohu.expense.data.remote.api.WebhookApi
import com.leohu.expense.data.repository.ExpenseRepositoryImpl
import com.leohu.expense.domain.repository.ExpenseRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ExpenseApplication : Application() {
    
    lateinit var repository: ExpenseRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "expense-db"
        ).build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofitAgnes = Retrofit.Builder()
            .baseUrl("https://apihub.agnes-ai.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val retrofitWebhook = Retrofit.Builder()
            .baseUrl("http://leohu.ddns.net:5678/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val agnesApi = retrofitAgnes.create(AgnesApi::class.java)
        val webhookApi = retrofitWebhook.create(WebhookApi::class.java)

        repository = ExpenseRepositoryImpl(applicationContext, db, agnesApi, webhookApi)
    }
}
