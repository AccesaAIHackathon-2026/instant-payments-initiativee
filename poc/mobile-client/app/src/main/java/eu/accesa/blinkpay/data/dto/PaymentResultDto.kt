package eu.accesa.blinkpay.data.dto

data class PaymentResultDto(
    val uetr: String,
    val status: String,       // "ACSC" = settled, "RJCT" = rejected
    val rejectReason: String?,
)
