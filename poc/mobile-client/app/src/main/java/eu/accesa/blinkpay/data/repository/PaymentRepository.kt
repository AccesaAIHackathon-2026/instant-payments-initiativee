package eu.accesa.blinkpay.data.repository

import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.api.dto.PaymentRequest
import eu.accesa.blinkpay.data.api.dto.PaymentResponse
import eu.accesa.blinkpay.data.api.dto.ScaRequest
import eu.accesa.blinkpay.data.api.dto.ScaResponse
import eu.accesa.blinkpay.data.model.QrPaymentData

class PaymentRepository {

    companion object {
        // Hardcoded debtor for POC — Alice's IBAN
        const val ALICE_IBAN = "DE89370400440532013001"
    }

    suspend fun initiatePayment(qrData: QrPaymentData): PaymentResponse {
        val request = PaymentRequest(
            debtorIBAN = ALICE_IBAN,
            creditorIBAN = qrData.creditorIban,
            amount = qrData.amount,
            currency = qrData.currency,
            reference = qrData.reference,
        )
        return ApiClient.bankApi.initiatePayment(request)
    }

    suspend fun confirmSca(uetr: String, challengeToken: String): ScaResponse {
        val request = ScaRequest(
            uetr = uetr,
            scaChallengeToken = challengeToken,
        )
        return ApiClient.bankApi.confirmSca(request)
    }
}
