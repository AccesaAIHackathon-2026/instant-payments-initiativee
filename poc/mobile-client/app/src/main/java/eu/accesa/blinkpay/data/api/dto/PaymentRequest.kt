package eu.accesa.blinkpay.data.api.dto

data class PaymentRequest(
    val debtorIBAN: String,
    val creditorIBAN: String? = null,
    val creditorAlias: String? = null,
    val amount: Double,
    val remittanceInfo: String? = null,
)
