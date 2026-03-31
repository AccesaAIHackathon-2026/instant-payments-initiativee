package eu.accesa.blinkpay.data.api

import eu.accesa.blinkpay.data.dto.AccountViewDto
import eu.accesa.blinkpay.data.dto.PaymentInitiatedResponseDto
import eu.accesa.blinkpay.data.dto.PaymentRequestDto
import eu.accesa.blinkpay.data.dto.PaymentResultDto
import eu.accesa.blinkpay.data.dto.ScaRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BankApi {

    @GET("bank/accounts/{iban}")
    suspend fun getAccount(@Path("iban") iban: String): AccountViewDto

    @POST("bank/pay")
    suspend fun initiatePayment(@Body request: PaymentRequestDto): PaymentInitiatedResponseDto

    @POST("bank/sca")
    suspend fun confirmSca(@Body request: ScaRequestDto): PaymentResultDto
}
