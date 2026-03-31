package eu.accesa.blinkpay.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.dto.AccountViewDto
import eu.accesa.blinkpay.data.dto.WalletTransferRequestDto
import eu.accesa.blinkpay.data.dto.WalletViewDto
import eu.accesa.blinkpay.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface AccountUiState {
    object Loading : AccountUiState
    data class Loaded(
        val iban: String,
        val holderName: String,
        val bankBalance: BigDecimal,
        val walletId: String?,
        val digitalEuroBalance: BigDecimal?,
        val transferring: Boolean = false,
        val transferError: String? = null,
    ) : AccountUiState
    data class Error(val message: String) : AccountUiState
}

class AccountViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState

    fun load(iban: String) {
        _uiState.value = AccountUiState.Loading
        viewModelScope.launch {
            try {
                val account: AccountViewDto = ApiClient.bankApi.getAccount(iban)
                val wallet: WalletViewDto? = account.walletId?.let {
                    runCatching { ApiClient.bankApi.getWallet(it) }.getOrNull()
                }
                _uiState.value = AccountUiState.Loaded(
                    iban = account.iban,
                    holderName = account.holderName,
                    bankBalance = account.bankBalance,
                    walletId = account.walletId,
                    digitalEuroBalance = wallet?.balance,
                )
            } catch (e: Exception) {
                _uiState.value = AccountUiState.Error(e.message ?: "Failed to load account")
            }
        }
    }

    fun topUp(amount: BigDecimal) = transfer(amount) { walletId ->
        ApiClient.bankApi.topUpWallet(walletId, WalletTransferRequestDto(amount))
    }

    fun redeem(amount: BigDecimal) = transfer(amount) { walletId ->
        ApiClient.bankApi.redeemWallet(walletId, WalletTransferRequestDto(amount))
    }

    private fun transfer(amount: BigDecimal, action: suspend (String) -> eu.accesa.blinkpay.data.dto.WalletTransferResponseDto) {
        val current = _uiState.value as? AccountUiState.Loaded ?: return
        val walletId = current.walletId ?: return
        _uiState.value = current.copy(transferring = true, transferError = null)
        viewModelScope.launch {
            try {
                val response = action(walletId)
                _uiState.value = current.copy(
                    bankBalance = response.bankBalance,
                    digitalEuroBalance = response.walletBalance,
                    transferring = false,
                    transferError = null,
                )
            } catch (e: Exception) {
                _uiState.value = current.copy(
                    transferring = false,
                    transferError = e.message ?: "Transfer failed",
                )
            }
        }
    }
}
