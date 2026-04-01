package eu.accesa.blinkpay.nfc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared state bridge between NFC services/callbacks and ViewModels.
 * HCE service and reader callback run on binder threads, so communication
 * with the UI layer goes through this singleton.
 */
object NfcBridge {

    // --- Receiver side (HCE) ---

    /** The payment request payload the HCE service should broadcast. */
    @Volatile
    var pendingRequestPayload: ByteArray? = null

    /** Emits when the sender has confirmed the transfer via NFC. */
    private val _confirmationReceived = MutableStateFlow<NfcPaymentConfirmation?>(null)
    val confirmationReceived: StateFlow<NfcPaymentConfirmation?> = _confirmationReceived

    fun onConfirmationReceived(confirmation: NfcPaymentConfirmation) {
        _confirmationReceived.value = confirmation
    }

    // --- Sender side (Reader) ---

    /** Emits when the reader has read a payment request from a receiver's HCE. */
    private val _requestRead = MutableStateFlow<NfcPaymentRequest?>(null)
    val requestRead: StateFlow<NfcPaymentRequest?> = _requestRead

    fun onRequestRead(request: NfcPaymentRequest) {
        _requestRead.value = request
    }

    // --- Reset ---

    fun resetReceiver() {
        pendingRequestPayload = null
        _confirmationReceived.value = null
    }

    fun resetSender() {
        _requestRead.value = null
    }
}
