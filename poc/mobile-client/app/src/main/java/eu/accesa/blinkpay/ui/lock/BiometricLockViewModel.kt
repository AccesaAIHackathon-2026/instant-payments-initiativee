package eu.accesa.blinkpay.ui.lock

import androidx.lifecycle.ViewModel
import eu.accesa.blinkpay.BuildConfig
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
// bank-a (BANK_API_BASE_URL):   Alice/Bob + audience PINs 1001-1010  (IBAN prefix 013)
// bank-b (BANK_B_API_BASE_URL): Charlie   + audience PINs 2001-2010  (IBAN prefix 014)
private val USER_PINS by lazy {
    val a = BuildConfig.BANK_API_BASE_URL
    val b = BuildConfig.BANK_B_API_BASE_URL
    mapOf(
        // Demo accounts
        "1111" to UserProfile("DE89370400440532013001", "Alice Consumer",   "+49111000001", a),
        "2222" to UserProfile("DE89370400440532013002", "Bob Consumer",     "+49111000002", a),
        "4444" to UserProfile("DE89370400440532014001", "Charlie Consumer", "+49222000001", b),
        // Audience — Bank A (PINs 1001-1010)
        "1001" to UserProfile("DE89370400440532013003", "Hans Mueller",     "+49111000003", a),
        "1002" to UserProfile("DE89370400440532013004", "Maria Schmidt",    "+49111000004", a),
        "1003" to UserProfile("DE89370400440532013005", "Klaus Weber",      "+49111000005", a),
        "1004" to UserProfile("DE89370400440532013006", "Anna Fischer",     "+49111000006", a),
        "1005" to UserProfile("DE89370400440532013007", "Thomas Meyer",     "+49111000007", a),
        "1006" to UserProfile("DE89370400440532013008", "Laura Wagner",     "+49111000008", a),
        "1007" to UserProfile("DE89370400440532013009", "Stefan Becker",    "+49111000009", a),
        "1008" to UserProfile("DE89370400440532013010", "Julia Hoffmann",   "+49111000010", a),
        "1009" to UserProfile("DE89370400440532013011", "Michael Schulz",   "+49111000011", a),
        "1010" to UserProfile("DE89370400440532013012", "Sophie Koch",      "+49111000012", a),
        // Audience — Bank B (PINs 2001-2010)
        "2001" to UserProfile("DE89370400440532014002", "Luca Rossi",       "+49222000002", b),
        "2002" to UserProfile("DE89370400440532014003", "Emma Laurent",     "+49222000003", b),
        "2003" to UserProfile("DE89370400440532014004", "Marco Bianchi",    "+49222000004", b),
        "2004" to UserProfile("DE89370400440532014005", "Sophie Dubois",    "+49222000005", b),
        "2005" to UserProfile("DE89370400440532014006", "Jan van der Berg", "+49222000006", b),
        "2006" to UserProfile("DE89370400440532014007", "Elena Popescu",    "+49222000007", b),
        "2007" to UserProfile("DE89370400440532014008", "Carlos Garcia",    "+49222000008", b),
        "2008" to UserProfile("DE89370400440532014009", "Ingrid Johansson", "+49222000009", b),
        "2009" to UserProfile("DE89370400440532014010", "Andrei Ionescu",   "+49222000010", b),
        "2010" to UserProfile("DE89370400440532014011", "Freya Nielsen",    "+49222000011", b),
    )
}

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
