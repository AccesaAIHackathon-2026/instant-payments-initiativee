package eu.accesa.blinkpay.ui.qr

import androidx.lifecycle.ViewModel
import eu.accesa.blinkpay.data.model.QrPaymentData
import eu.accesa.blinkpay.data.model.parseEpcQr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QrScanViewModel : ViewModel() {

    private val _scannedPayment = MutableStateFlow<QrPaymentData?>(null)
    val scannedPayment: StateFlow<QrPaymentData?> = _scannedPayment

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Called from the ML Kit barcode callback. Thread-safe via atomic CAS on StateFlow. */
    fun onQrDetected(rawValue: String) {
        // Only process the first successful scan
        if (_scannedPayment.value != null) return

        val parsed = parseEpcQr(rawValue)
        if (parsed != null) {
            _scannedPayment.value = parsed
        } else {
            _error.value = "Not a valid payment QR code"
        }
    }

    fun clearError() {
        _error.value = null
    }
}
