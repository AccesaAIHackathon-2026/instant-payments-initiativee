package eu.accesa.blinkpay.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.dto.PaymentRequestDto
import eu.accesa.blinkpay.data.dto.ScaRequestDto
import eu.accesa.blinkpay.data.model.QrPaymentData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Alice's hardcoded IBAN for the POC. */
private const val ALICE_IBAN = "DE89370400440532013001"
private const val STUB_PIN = "1234"

sealed interface PaymentUiState {
    data class Confirming(val payment: QrPaymentData) : PaymentUiState
    object Processing : PaymentUiState
    data class Success(val uetr: String) : PaymentUiState
    data class Failed(val reason: String) : PaymentUiState
}

class PaymentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState?>(null)
    val uiState: StateFlow<PaymentUiState?> = _uiState

    fun setPayment(payment: QrPaymentData) {
        _uiState.value = PaymentUiState.Confirming(payment)
    }

    /**
     * Executes the full payment flow:
     * 1. POST /bank/pay  → receive SCA challenge
     * 2. POST /bank/sca  → confirm with stub PIN → settlement result
     */
    fun confirmPayment(payment: QrPaymentData) {
        _uiState.value = PaymentUiState.Processing

        viewModelScope.launch {
            try {
                val api = ApiClient.bankApi

                // Step 1 — initiate payment
                val initiated = api.initiatePayment(
                    PaymentRequestDto(
                        debtorIBAN = ALICE_IBAN,
                        creditorIBAN = payment.creditorIban,
                        amount = payment.amount,
                        remittanceInfo = payment.reference,
                    )
                )

                // Step 2 — SCA confirmation with stub PIN
                val result = api.confirmSca(
                    ScaRequestDto(
                        uetr = initiated.uetr,
                        pin = STUB_PIN,
                    )
                )

                if (result.status == "ACSC") {
                    _uiState.value = PaymentUiState.Success(uetr = result.uetr)
                } else {
                    _uiState.value = PaymentUiState.Failed(
                        reason = result.rejectReason ?: "Payment rejected"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PaymentUiState.Failed(
                    reason = e.message ?: "Network error"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = null
    }
}
