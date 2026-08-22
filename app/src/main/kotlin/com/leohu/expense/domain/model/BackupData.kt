package com.leohu.expense.domain.model

data class BackupData(
    val creditCards: List<CreditCard>,
    val eWalletAccounts: List<EWalletAccount>
)
