package eu.accesa.blinkpay.data.api.dto

data class PaymentRequest(
    val debtorIBAN: String,
    val creditorIBAN: String? = null,
    val creditorAlias: String? = null,
    val amount: Double,
    val creditorReference: String? = null,  // ISO 11649 from QR line 9; null for P2P
    val remittanceInfo: String? = null,
)
