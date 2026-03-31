package eu.accesa.blinkpay.ui.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.biometric.BiometricAvailability
import eu.accesa.blinkpay.biometric.BiometricHelper
import eu.accesa.blinkpay.util.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LockUiState(
    val useBiometric: Boolean = true,
    val pinInput: String = "",
    val pinError: Boolean = false,
    val authFailed: Boolean = false,
)

class BiometricLockViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState

    private var biometricHelper: BiometricHelper? = null

    fun initialize(activity: FragmentActivity) {
        biometricHelper = BiometricHelper(activity)
        val availability = biometricHelper!!.checkAvailability()
        _uiState.value = _uiState.value.copy(
            useBiometric = availability == BiometricAvailability.AVAILABLE
        )
    }

    fun attemptBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        val helper = BiometricHelper(activity)
        viewModelScope.launch {
            val success = helper.authenticate(
                title = "Unlock BlinkPay",
                subtitle = "Use your fingerprint or face to unlock",
            )
            if (success) {
                ServiceLocator.unlock()
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(authFailed = true)
            }
        }
    }

    fun onPinDigitEntered(digit: Char) {
        val current = _uiState.value.pinInput
        if (current.length < 4) {
            _uiState.value = _uiState.value.copy(
                pinInput = current + digit,
                pinError = false,
                authFailed = false,
            )
        }
    }

    fun onPinDelete() {
        val current = _uiState.value.pinInput
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                pinInput = current.dropLast(1),
                pinError = false,
            )
        }
    }

    fun verifyPin(onSuccess: () -> Unit) {
        if (_uiState.value.pinInput == "1234") {
            ServiceLocator.unlock()
            _uiState.value = _uiState.value.copy(pinInput = "", pinError = false)
            onSuccess()
        } else {
            _uiState.value = _uiState.value.copy(pinInput = "", pinError = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(authFailed = false, pinError = false)
    }
}
