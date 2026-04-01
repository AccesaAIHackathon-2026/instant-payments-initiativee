package eu.accesa.blinkpay.ui.lock

import androidx.lifecycle.ViewModel
import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.util.ServiceLocator
import eu.accesa.blinkpay.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private data class UserProfile(
    val iban: String,
    val holderName: String,
    val phoneAlias: String?,
    val bankBaseUrl: String,
)

// Pre-seeded demo accounts — PIN selects which user the app operates as.
// bank-a (8080): Alice, Bob, Retail Store GmbH  (IBAN prefix 013)
// bank-b (8082): Charlie, Metro Market          (IBAN prefix 014)
private val USER_PINS = mapOf(
    "1111" to UserProfile("DE89370400440532013001", "Alice Consumer",   "+49111000001", "http://10.0.2.2:8080/"),
    "2222" to UserProfile("DE89370400440532013002", "Bob Consumer",     "+49111000002", "http://10.0.2.2:8080/"),
    "4444" to UserProfile("DE89370400440532014001", "Charlie Consumer", "+49222000001", "http://10.0.2.2:8082/"),
)

data class LockUiState(
    val pinInput: String = "",
    val pinError: Boolean = false,
)

class BiometricLockViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState

    fun onPinDigitEntered(digit: Char) {
        val current = _uiState.value.pinInput
        if (current.length < 4) {
            _uiState.value = _uiState.value.copy(
                pinInput = current + digit,
                pinError = false,
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
        val profile = USER_PINS[_uiState.value.pinInput]
        if (profile != null) {
            UserSession.save(
                iban = profile.iban,
                holderName = profile.holderName,
                phoneAlias = profile.phoneAlias,
                walletId = null,
                bankBaseUrl = profile.bankBaseUrl,
            )
            ApiClient.onSessionChanged()
            ServiceLocator.unlock()
            _uiState.value = LockUiState()
            onSuccess()
        } else {
            _uiState.value = _uiState.value.copy(pinInput = "", pinError = true)
        }
    }
}
