package eu.accesa.blinkpay.data.repository

import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.api.dto.PaymentInitiatedResponse
import eu.accesa.blinkpay.data.api.dto.PaymentRequest
import eu.accesa.blinkpay.data.api.dto.PaymentResult
import eu.accesa.blinkpay.data.api.dto.ScaRequest
import eu.accesa.blinkpay.data.model.QrPaymentData

class PaymentRepository {

    companion object {
        const val ALICE_IBAN = "DE89370400440532013001"
    }

    suspend fun initiatePayment(qrData: QrPaymentData): PaymentInitiatedResponse {
        val request = PaymentRequest(
            debtorIBAN = ALICE_IBAN,
            creditorIBAN = qrData.creditorIban,
            amount = qrData.amount,
            remittanceInfo = qrData.reference,
        )
        return ApiClient.bankApi.initiatePayment(request)
    }

    suspend fun confirmSca(uetr: String): PaymentResult {
        val request = ScaRequest(uetr = uetr)
        return ApiClient.bankApi.confirmSca(request)
    }
}
