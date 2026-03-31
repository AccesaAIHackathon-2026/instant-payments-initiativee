package eu.accesa.blinkpay.ui.qr

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import eu.accesa.blinkpay.data.model.QrPaymentData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class QrScanState {
    data object Scanning : QrScanState()
    data class Scanned(val data: QrPaymentData) : QrScanState()
    data class Error(val message: String) : QrScanState()
}

class QrScanViewModel : ViewModel() {

    private val _state = MutableStateFlow<QrScanState>(QrScanState.Scanning)
    val state: StateFlow<QrScanState> = _state

    private val gson = Gson()
    private var hasScanned = false

    fun onQrCodeDetected(rawValue: String) {
        if (hasScanned) return
        hasScanned = true

        try {
            val data = gson.fromJson(rawValue, QrPaymentData::class.java)
            if (data.creditorIban.isNullOrBlank() || data.amount <= 0) {
                _state.value = QrScanState.Error("Invalid QR code: missing payment details")
                hasScanned = false
                return
            }
            _state.value = QrScanState.Scanned(data)
        } catch (e: Exception) {
            _state.value = QrScanState.Error("Invalid QR code format")
            hasScanned = false
        }
    }

    fun resetScanner() {
        hasScanned = false
        _state.value = QrScanState.Scanning
    }
}
