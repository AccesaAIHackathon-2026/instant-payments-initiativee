package eu.accesa.blinkpay.data.api.dto

data class PaymentResponse(
    val uetr: String,
    val status: String,
    val scaChallengeToken: String? = null,
    val createdAt: String? = null,
)
