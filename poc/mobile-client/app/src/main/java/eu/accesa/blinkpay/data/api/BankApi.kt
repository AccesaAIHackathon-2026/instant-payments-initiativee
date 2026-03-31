package eu.accesa.blinkpay.data.api

import eu.accesa.blinkpay.data.api.dto.PaymentRequest
import eu.accesa.blinkpay.data.api.dto.PaymentResponse
import eu.accesa.blinkpay.data.api.dto.ScaRequest
import eu.accesa.blinkpay.data.api.dto.ScaResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BankApi {

    @POST("/bank/pay")
    suspend fun initiatePayment(@Body request: PaymentRequest): PaymentResponse

    @POST("/bank/sca")
    suspend fun confirmSca(@Body request: ScaRequest): ScaResponse
}
