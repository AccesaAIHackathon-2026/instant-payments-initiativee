package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class WalletViewDto(
    val walletId: String,
    val ownerIban: String,
    val balance: BigDecimal,
)
