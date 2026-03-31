package eu.accesa.blinkpay.ui.payment

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.biometric.BiometricAvailability
import eu.accesa.blinkpay.biometric.BiometricHelper
import eu.accesa.blinkpay.data.model.QrPaymentData
import eu.accesa.blinkpay.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PaymentState {
    data object Idle : PaymentState()
    data object Authenticating : PaymentState()
    data object Processing : PaymentState()
    data class Success(val uetr: String) : PaymentState()
    data class Failed(val message: String) : PaymentState()
}

class PaymentViewModel : ViewModel() {

    private val _state = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val state: StateFlow<PaymentState> = _state

    private val repository = PaymentRepository()

    fun confirmPayment(activity: FragmentActivity, qrData: QrPaymentData) {
        viewModelScope.launch {
            _state.value = PaymentState.Authenticating

            val biometricHelper = BiometricHelper(activity)
            val useBiometric = biometricHelper.checkAvailability() == BiometricAvailability.AVAILABLE

            val authenticated = if (useBiometric) {
                biometricHelper.authenticate(
                    title = "Confirm Payment",
                    subtitle = "Verify to pay €${"%.2f".format(qrData.amount)} to ${qrData.creditorName}",
                )
            } else {
                // PIN fallback handled by UI — if we reach here, PIN was already verified
                true
            }

            if (!authenticated) {
                _state.value = PaymentState.Failed("Authentication cancelled")
                return@launch
            }

            executePayment(qrData)
        }
    }

    fun confirmPaymentWithPin(qrData: QrPaymentData, pin: String): Boolean {
        if (pin != "1234") return false

        viewModelScope.launch {
            executePayment(qrData)
        }
        return true
    }

    private suspend fun executePayment(qrData: QrPaymentData) {
        _state.value = PaymentState.Processing

        try {
            // Step 1: Initiate payment — POST /bank/pay
            val initiated = repository.initiatePayment(qrData)

            // Step 2: Confirm SCA — POST /bank/sca with uetr + pin "1234"
            val result = repository.confirmSca(uetr = initiated.uetr)

            // Step 3: Check result
            if (result.status == "ACSC") {
                _state.value = PaymentState.Success(uetr = result.uetr)
            } else {
                val reason = result.rejectReason ?: "Unknown"
                _state.value = PaymentState.Failed("Payment rejected: $reason")
            }
        } catch (e: Exception) {
            _state.value = PaymentState.Failed(
                e.message ?: "Payment failed. Please try again."
            )
        }
    }

    fun reset() {
        _state.value = PaymentState.Idle
    }
}
