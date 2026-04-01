package eu.accesa.blinkpay.ui.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.data.repository.DigitalEuroLedger
import eu.accesa.blinkpay.nfc.NfcBridge
import eu.accesa.blinkpay.nfc.NfcPayloadCodec
import eu.accesa.blinkpay.nfc.NfcPaymentRequest
import eu.accesa.blinkpay.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface NfcReceiveUiState {
    object EnteringAmount : NfcReceiveUiState
    data class Waiting(val amount: BigDecimal) : NfcReceiveUiState
    data class Received(
        val amount: BigDecimal,
        val senderName: String,
        val senderIban: String,
    ) : NfcReceiveUiState
    data class Error(val message: String) : NfcReceiveUiState
}

class NfcReceiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NfcReceiveUiState>(NfcReceiveUiState.EnteringAmount)
    val uiState: StateFlow<NfcReceiveUiState> = _uiState

    fun startReceiving(amount: BigDecimal) {
        // Prepare the HCE payload
        val request = NfcPaymentRequest(
            amount = amount,
            receiverName = UserSession.holderName,
            receiverIban = UserSession.iban,
        )
        val payload = NfcPayloadCodec.encodeRequest(request)
        NfcBridge.pendingRequestPayload = payload

        _uiState.value = NfcReceiveUiState.Waiting(amount)

        // Listen for confirmation from the sender
        viewModelScope.launch {
            NfcBridge.confirmationReceived.collect { confirmation ->
                if (confirmation != null && confirmation.confirmed) {
                    // Credit local ledger
                    DigitalEuroLedger.credit(
                        amount = amount,
                        counterpartyName = confirmation.senderName,
                        counterpartyIban = confirmation.senderIban,
                    )
                    _uiState.value = NfcReceiveUiState.Received(
                        amount = amount,
                        senderName = confirmation.senderName,
                        senderIban = confirmation.senderIban,
                    )
                }
            }
        }
    }

    fun reset() {
        NfcBridge.resetReceiver()
        _uiState.value = NfcReceiveUiState.EnteringAmount
    }

    override fun onCleared() {
        super.onCleared()
        NfcBridge.resetReceiver()
    }
}
