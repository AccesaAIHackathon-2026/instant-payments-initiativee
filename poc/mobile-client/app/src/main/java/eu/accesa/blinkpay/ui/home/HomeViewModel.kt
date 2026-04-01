package eu.accesa.blinkpay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.repository.DigitalEuroLedger
import eu.accesa.blinkpay.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Loaded(
        val holderName: String,
        val iban: String,
        val bankBalance: BigDecimal,
        val digitalEuroBalance: BigDecimal?,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val account = ApiClient.bankApi.getAccount(UserSession.iban)
                val wallet = account.walletId?.let {
                    runCatching { ApiClient.bankApi.getWallet(it) }.getOrNull()
                }

                // Persist walletId so OfflineSyncWorker can use it
                if (account.walletId != null) {
                    UserSession.save(
                        iban = account.iban,
                        holderName = account.holderName,
                        phoneAlias = UserSession.phoneAlias,
                        walletId = account.walletId,
                        bankBaseUrl = UserSession.bankBaseUrl,
                    )
                }

                // Initialize the offline ledger with server balance
                wallet?.balance?.let { DigitalEuroLedger.initialize(it) }

                _uiState.value = HomeUiState.Loaded(
                    holderName = account.holderName,
                    iban = account.iban,
                    bankBalance = account.bankBalance,
                    digitalEuroBalance = wallet?.balance,
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load account")
            }
        }
    }
}
