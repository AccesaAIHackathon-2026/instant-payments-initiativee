package eu.accesa.blinkpay.data.repository

import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.api.dto.PaymentInitiatedResponse
import eu.accesa.blinkpay.data.api.dto.PaymentRequest
import eu.accesa.blinkpay.data.api.dto.PaymentResult
import eu.accesa.blinkpay.data.api.dto.ScaRequest
import eu.accesa.blinkpay.data.model.QrPaymentData
import eu.accesa.blinkpay.util.UserSession

class PaymentRepository {

    suspend fun initiatePayment(qrData: QrPaymentData): PaymentInitiatedResponse {
        val request = PaymentRequest(
            debtorIBAN = UserSession.iban,
            creditorIBAN = qrData.creditorIban,
            amount = qrData.amount.toDouble(),
            creditorReference = qrData.creditorReference,
            remittanceInfo = qrData.reference,
        )
        return ApiClient.bankApi.initiatePayment(request)
    }

    suspend fun confirmSca(uetr: String): PaymentResult {
        val request = ScaRequest(uetr = uetr)
        return ApiClient.bankApi.confirmSca(request)
    }
}
