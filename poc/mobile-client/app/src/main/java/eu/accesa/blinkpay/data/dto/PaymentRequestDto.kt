package eu.accesa.blinkpay.data.dto

import java.math.BigDecimal

data class PaymentRequestDto(
    val debtorIBAN: String,
    val creditorIBAN: String,
    val creditorAlias: String? = null,
    val amount: BigDecimal,
    val remittanceInfo: String,
)
