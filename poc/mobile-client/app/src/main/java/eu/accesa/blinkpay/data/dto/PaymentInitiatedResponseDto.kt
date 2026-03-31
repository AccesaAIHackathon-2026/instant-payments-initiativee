package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class PaymentInitiatedResponseDto(
    val uetr: String,
    val scaChallengeToken: String,
    val creditorName: String,
    val creditorIBAN: String,
    val amount: BigDecimal,
)
