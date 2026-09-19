package com.leohu.expense.data.remote.db

import java.util.UUID

data class DbProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 5432,
    val database: String = "postgres",
    val username: String = "",
    val password: String = ""
) {
    // 生成連接資訊（僅用於顯示）
    fun getConnectionString(): String = "$host:$port/$database"
    
    // 判斷是否為 n8n webhook
    fun isN8nWebhook(): Boolean = port == 5678
    
    // 獲取 API Base URL
    fun getBaseUrl(): String = "http://$host:$port/"
}
