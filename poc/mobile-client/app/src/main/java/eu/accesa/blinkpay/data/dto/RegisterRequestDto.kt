package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class RegisterRequestDto(
    val accountType: String,           // "CONSUMER"
    val holderName: String,
    val phoneAlias: String?,           // CONSUMER only
    val bankBalance: BigDecimal?,      // demo convenience — null defaults to €0 on server
    val digitalEuroBalance: BigDecimal?, // demo convenience — null defaults to €0 on server
)
