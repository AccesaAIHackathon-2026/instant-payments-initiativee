package eu.accesa.blinkpay.data.api.dto

data class ScaResponse(
    val uetr: String,
    val status: String,
    val settledAt: String? = null,
)
