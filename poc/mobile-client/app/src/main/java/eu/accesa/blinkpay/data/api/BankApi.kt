package eu.accesa.blinkpay.data.api

import eu.accesa.blinkpay.data.api.dto.PaymentInitiatedResponse
import eu.accesa.blinkpay.data.api.dto.PaymentRequest
import eu.accesa.blinkpay.data.api.dto.PaymentResult
import eu.accesa.blinkpay.data.api.dto.ScaRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface BankApi {

    @POST("/bank/pay")
    suspend fun initiatePayment(@Body request: PaymentRequest): PaymentInitiatedResponse

    @POST("/bank/sca")
    suspend fun confirmSca(@Body request: ScaRequest): PaymentResult
}
