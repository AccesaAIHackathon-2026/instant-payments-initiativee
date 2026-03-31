package eu.accesa.blinkpay.data.api.dto

data class PaymentRequest(
    val debtorIBAN: String,
    val creditorIBAN: String,
    val amount: Double,
    val currency: String,
    val reference: String,
)
