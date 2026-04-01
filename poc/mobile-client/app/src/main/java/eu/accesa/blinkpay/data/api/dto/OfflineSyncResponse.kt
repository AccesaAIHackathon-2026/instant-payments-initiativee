package eu.accesa.blinkpay.data.api.dto

import java.math.BigDecimal

data class OfflineSyncResponse(
    val walletId: String,
    val walletBalance: BigDecimal,
    val acceptedTransactionIds: List<String>,
    val duplicateTransactionIds: List<String>,
)
