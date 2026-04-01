package eu.accesa.blinkpay.data.api.dto

import java.math.BigDecimal

data class OfflineSyncRequest(
    val transactions: List<OfflineTransactionEntry>,
)

data class OfflineTransactionEntry(
    val transactionId: String,
    val counterpartyIban: String,
    val amount: BigDecimal,
    val direction: String,   // "SEND" or "RECEIVE"
    val timestamp: String,   // ISO-8601 instant
)
