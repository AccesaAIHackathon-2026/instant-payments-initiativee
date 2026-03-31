package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class RegisterResponseDto(
    val iban: String,
    val holderName: String,
    val accountType: String,
    val phoneAlias: String?,
    val bankBalance: BigDecimal,
    val walletId: String?,
)
