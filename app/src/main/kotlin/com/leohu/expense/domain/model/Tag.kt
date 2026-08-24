package com.leohu.expense.domain.model

data class Tag(
    val id: String,              // UUID
    val name: String,            // Tag 名稱
    val color: String,           // Tag 顏色（十六進位，例如 "#FFF9C4"）
    val createdAt: Long = System.currentTimeMillis()
)