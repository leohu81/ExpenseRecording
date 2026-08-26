package com.leohu.expense.data.mapper

import com.leohu.expense.data.local.entity.*
import com.leohu.expense.domain.model.*

fun SourceImageEntity.toDomain() = SourceImage(
    id = id,
    localPath = localPath,
    createdAt = createdAt,
    status = status,
    retryCount = retryCount,
    lastError = lastError,
    preDescription = preDescription,
    tags = tags?.let { if (it.isEmpty()) emptyList() else it.split(",").filter { s -> s.isNotEmpty() } } ?: emptyList()
)

fun SourceImage.toEntity() = SourceImageEntity(
    id = id,
    localPath = localPath,
    createdAt = createdAt,
    status = status,
    retryCount = retryCount,
    lastError = lastError,
    preDescription = preDescription,
    tags = tags.joinToString(",")
)

fun PaymentRecordEntity.toDomain() = PaymentRecord(
    id = id,
    sourceImageId = sourceImageId,
    method = method,
    account = account,
    cardLast4 = cardLast4,
    amount = amount,
    amountTwd = amountTwd,
    currency = currency,
    consumeDate = consumeDate,
    description = description,
    status = status,
    tags = tags?.let { if (it.isEmpty()) emptyList() else it.split(",").filter { s -> s.isNotEmpty() } } ?: emptyList(),
    createdAt = createdAt,
    approvedAt = approvedAt
)

fun PaymentRecord.toEntity() = PaymentRecordEntity(
    id = id,
    sourceImageId = sourceImageId,
    method = method,
    account = account,
    cardLast4 = cardLast4,
    amount = amount,
    amountTwd = amountTwd,
    currency = currency,
    consumeDate = consumeDate,
    description = description,
    status = status,
    tags = tags.joinToString(","),
    createdAt = createdAt,
    approvedAt = approvedAt
)

fun CreditCardEntity.toDomain() = CreditCard(id, name, fullCardNumber, last4, issuer, isActive)
fun CreditCard.toEntity() = CreditCardEntity(id, name, fullCardNumber, last4, issuer, isActive)

fun EWalletAccountEntity.toDomain() = EWalletAccount(id, name, keywords, isActive)
fun EWalletAccount.toEntity() = EWalletAccountEntity(id, name, keywords, isActive)

fun TagEntity.toDomain() = Tag(id, name, color, createdAt)
fun Tag.toEntity() = TagEntity(id, name, color, createdAt)
