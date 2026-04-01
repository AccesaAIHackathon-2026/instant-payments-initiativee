package eu.accesa.blinkpay.data.api

import eu.accesa.blinkpay.data.api.dto.OfflineSyncRequest
import eu.accesa.blinkpay.data.api.dto.OfflineSyncResponse
import eu.accesa.blinkpay.data.api.dto.PaymentInitiatedResponse
import eu.accesa.blinkpay.data.api.dto.PaymentRequest
import eu.accesa.blinkpay.data.api.dto.PaymentResult
import eu.accesa.blinkpay.data.api.dto.ScaRequest
import eu.accesa.blinkpay.data.dto.AccountViewDto
import eu.accesa.blinkpay.data.dto.WalletTransferRequestDto
import eu.accesa.blinkpay.data.dto.WalletTransferResponseDto
import eu.accesa.blinkpay.data.dto.WalletViewDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BankApi {

    @GET("bank/accounts/{iban}")
    suspend fun getAccount(@Path("iban") iban: String): AccountViewDto

    @GET("bank/wallet/{walletId}")
    suspend fun getWallet(@Path("walletId") walletId: String): WalletViewDto

    @POST("bank/wallet/{walletId}/topup")
    suspend fun topUpWallet(@Path("walletId") walletId: String, @Body request: WalletTransferRequestDto): WalletTransferResponseDto

    @POST("bank/wallet/{walletId}/redeem")
    suspend fun redeemWallet(@Path("walletId") walletId: String, @Body request: WalletTransferRequestDto): WalletTransferResponseDto

    @POST("bank/pay")
    suspend fun initiatePayment(@Body request: PaymentRequest): PaymentInitiatedResponse

    @POST("bank/sca")
    suspend fun confirmSca(@Body request: ScaRequest): PaymentResult

    @POST("bank/wallet/{walletId}/sync-offline-transactions")
    suspend fun syncOfflineTransactions(
        @Path("walletId") walletId: String,
        @Body request: OfflineSyncRequest,
    ): OfflineSyncResponse
}
