package com.leohu.expense.data.mapper

import com.leohu.expense.data.local.entity.*
import com.leohu.expense.domain.model.*

fun SourceImageEntity.toDomain() = SourceImage(id, localPath, createdAt, status, retryCount, lastError)
fun SourceImage.toEntity() = SourceImageEntity(id, localPath, createdAt, status, retryCount, lastError)

fun PaymentRecordEntity.toDomain() = PaymentRecord(id, sourceImageId, method, account, cardLast4, amount, currency, consumeDate, description, status, createdAt, approvedAt)
fun PaymentRecord.toEntity() = PaymentRecordEntity(id, sourceImageId, method, account, cardLast4, amount, currency, consumeDate, description, status, createdAt, approvedAt)

fun CreditCardEntity.toDomain() = CreditCard(id, name, fullCardNumber, last4, issuer, isActive)
fun CreditCard.toEntity() = CreditCardEntity(id, name, fullCardNumber, last4, issuer, isActive)

fun EWalletAccountEntity.toDomain() = EWalletAccount(id, name, keywords, isActive)
fun EWalletAccount.toEntity() = EWalletAccountEntity(id, name, keywords, isActive)
