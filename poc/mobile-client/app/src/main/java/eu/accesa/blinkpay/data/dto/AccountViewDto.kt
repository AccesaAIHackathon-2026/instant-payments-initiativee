package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class AccountViewDto(
    val iban: String,
    val holderName: String,
    val bankBalance: BigDecimal,
    val digitalEuroBalance: BigDecimal,
)
