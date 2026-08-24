package com.leohu.expense.domain.model

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("creditCards") val creditCards: List<CreditCard>,
    @SerializedName("eWalletAccounts") val eWalletAccounts: List<EWalletAccount>
)
