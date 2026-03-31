package eu.accesa.blinkpay.data.api.dto

data class PaymentResult(
    val uetr: String,
    val status: String,
    val rejectReason: String? = null,
)
