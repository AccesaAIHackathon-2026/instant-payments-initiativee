package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class WalletTransferResponseDto(
    val walletId: String,
    val ownerIban: String,
    val walletBalance: BigDecimal,
    val bankBalance: BigDecimal,
)
