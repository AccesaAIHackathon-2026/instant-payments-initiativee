package eu.accesa.blinkpay.ui.nfc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.accesa.blinkpay.data.repository.DigitalEuroLedger
import eu.accesa.blinkpay.nfc.NfcBridge
import eu.accesa.blinkpay.nfc.NfcPaymentConfirmation
import eu.accesa.blinkpay.nfc.NfcPaymentRequest
import eu.accesa.blinkpay.nfc.NfcReaderCallback
import eu.accesa.blinkpay.sync.OfflineSyncWorker
import eu.accesa.blinkpay.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface NfcSendUiState {
    object WaitingForTap : NfcSendUiState
    data class Confirming(
        val amount: BigDecimal,
        val receiverName: String,
        val receiverIban: String,
    ) : NfcSendUiState
    object Authenticating : NfcSendUiState
    data class Success(
        val amount: BigDecimal,
        val receiverName: String,
    ) : NfcSendUiState
    data class InsufficientBalance(
        val amount: BigDecimal,
        val available: BigDecimal,
    ) : NfcSendUiState
    data class Error(val message: String) : NfcSendUiState
}

class NfcSendViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<NfcSendUiState>(NfcSendUiState.WaitingForTap)
    val uiState: StateFlow<NfcSendUiState> = _uiState

    private var pendingRequest: NfcPaymentRequest? = null

    val readerCallback = NfcReaderCallback(
        onRequestRead = { request ->
            pendingRequest = request
            NfcBridge.onRequestRead(request)
            _uiState.value = NfcSendUiState.Confirming(
                amount = request.amount,
                receiverName = request.receiverName,
                receiverIban = request.receiverIban,
            )
        },
        onError = { message ->
            _uiState.value = NfcSendUiState.Error(message)
        },
    )

    fun onAuthenticationSuccess() {
        val request = pendingRequest ?: return

        // Check balance
        if (!DigitalEuroLedger.canDebit(request.amount)) {
            _uiState.value = NfcSendUiState.InsufficientBalance(
                amount = request.amount,
                available = DigitalEuroLedger.balance.value,
            )
            readerCallback.close()
            return
        }

        _uiState.value = NfcSendUiState.Authenticating

        viewModelScope.launch(Dispatchers.IO) {
            val confirmation = NfcPaymentConfirmation(
                senderName = UserSession.holderName,
                senderIban = UserSession.iban,
                confirmed = true,
            )

            val acked = readerCallback.sendConfirmation(confirmation)

            if (acked) {
                // Debit local ledger
                DigitalEuroLedger.debit(
                    amount = request.amount,
                    counterpartyName = request.receiverName,
                    counterpartyIban = request.receiverIban,
                )
                _uiState.value = NfcSendUiState.Success(
                    amount = request.amount,
                    receiverName = request.receiverName,
                )
                OfflineSyncWorker.schedule(getApplication())
            } else {
                _uiState.value = NfcSendUiState.Error(
                    "Connection lost. Please tap phones again."
                )
            }
        }
    }

    fun onAuthenticationFailed() {
        val request = pendingRequest
        if (request != null) {
            _uiState.value = NfcSendUiState.Confirming(
                amount = request.amount,
                receiverName = request.receiverName,
                receiverIban = request.receiverIban,
            )
        } else {
            _uiState.value = NfcSendUiState.WaitingForTap
        }
    }

    fun reset() {
        readerCallback.close()
        NfcBridge.resetSender()
        pendingRequest = null
        _uiState.value = NfcSendUiState.WaitingForTap
    }

    override fun onCleared() {
        super.onCleared()
        readerCallback.close()
        NfcBridge.resetSender()
    }
}
