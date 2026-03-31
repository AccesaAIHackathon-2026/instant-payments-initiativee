package eu.accesa.blinkpay.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.dto.RegisterRequestDto
import eu.accesa.blinkpay.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface RegistrationUiState {
    object Idle : RegistrationUiState
    object Loading : RegistrationUiState
    object Success : RegistrationUiState
    data class Error(val message: String) : RegistrationUiState
}

class RegistrationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState

    fun register(
        holderName: String,
        phoneAlias: String,
        initialBankBalance: BigDecimal,
        initialDeBalance: BigDecimal,
    ) {
        if (holderName.isBlank()) {
            _uiState.value = RegistrationUiState.Error("Name is required")
            return
        }
        if (phoneAlias.isBlank()) {
            _uiState.value = RegistrationUiState.Error("Phone number is required")
            return
        }

        _uiState.value = RegistrationUiState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.bankApi.register(
                    RegisterRequestDto(
                        accountType = "CONSUMER",
                        holderName = holderName.trim(),
                        phoneAlias = phoneAlias.trim(),
                        bankBalance = initialBankBalance,
                        digitalEuroBalance = initialDeBalance,
                    )
                )
                UserSession.save(
                    iban = response.iban,
                    holderName = response.holderName,
                    phoneAlias = response.phoneAlias,
                    walletId = response.walletId,
                )
                _uiState.value = RegistrationUiState.Success
            } catch (e: Exception) {
                _uiState.value = RegistrationUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun clearError() {
        _uiState.value = RegistrationUiState.Idle
    }
}
