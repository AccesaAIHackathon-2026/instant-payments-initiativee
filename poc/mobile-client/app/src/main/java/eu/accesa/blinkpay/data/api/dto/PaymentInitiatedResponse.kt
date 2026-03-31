package eu.accesa.blinkpay.data.api.dto

data class PaymentInitiatedResponse(
    val uetr: String,
    val creditorName: String,
    val creditorIBAN: String,
    val amount: Double,
)
