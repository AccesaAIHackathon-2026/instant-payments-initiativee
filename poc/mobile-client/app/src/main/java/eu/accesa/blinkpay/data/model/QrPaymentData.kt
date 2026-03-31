package eu.accesa.blinkpay.data.model

data class QrPaymentData(
    val creditorIban: String,
    val creditorName: String,
    val amount: Double,
    val currency: String,
    val reference: String,
)
