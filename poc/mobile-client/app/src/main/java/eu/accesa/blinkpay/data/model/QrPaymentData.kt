package eu.accesa.blinkpay.data.model

import java.math.BigDecimal

/**
 * Domain model for a parsed EPC069-12 (GiroCode) QR code payload.
 */
data class QrPaymentData(
    val creditorName: String,
    val creditorIban: String,
    val amount: BigDecimal,
    val currency: String,
    val creditorReference: String?,  // ISO 11649 structured reference from QR line 9; null for P2P
    val reference: String,           // unstructured remittance info from QR line 10
)

/**
 * Parses an EPC069-12 (GiroCode) QR code string into [QrPaymentData].
 *
 * Expected line layout (newline-separated):
 *   0  BCD           – service tag
 *   1  002           – version
 *   2  1             – character set (UTF-8)
 *   3  SCT           – SEPA Credit Transfer
 *   4  (BIC)         – optional
 *   5  creditorName
 *   6  creditorIBAN
 *   7  CURRamount    – e.g. "EUR25.00"
 *   8  (purpose)     – optional
 *   9  (structured remittance ref) – optional
 *  10  unstructured remittance info (reference)
 */
fun parseEpcQr(raw: String): QrPaymentData? {
    val lines = raw.split("\n")
    if (lines.size < 8) return null
    if (lines[0] != "BCD") return null
    if (lines[3] != "SCT") return null

    val creditorName = lines[5]
    val creditorIban = lines[6]

    // Amount field: "EUR25.00" → currency = "EUR", amount = 25.00
    val amountLine = lines[7]
    if (amountLine.length < 4) return null
    val currency = amountLine.substring(0, 3)
    val amount = amountLine.substring(3).toBigDecimalOrNull() ?: return null

    val creditorReference = lines.getOrElse(9) { "" }.ifBlank { null }
    val reference = lines.getOrElse(10) { "" }

    return QrPaymentData(
        creditorName = creditorName,
        creditorIban = creditorIban,
        amount = amount,
        currency = currency,
        creditorReference = creditorReference,
        reference = reference,
    )
}
