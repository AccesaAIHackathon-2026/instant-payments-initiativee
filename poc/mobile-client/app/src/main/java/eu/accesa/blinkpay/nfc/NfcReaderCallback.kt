package eu.accesa.blinkpay.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import java.io.IOException

/**
 * NFC reader callback running on the **sender's** device.
 *
 * When a tag (receiver's HCE) is discovered:
 * 1. SELECT AID → read payment request
 * 2. Keep connection open so we can send CONFIRM after user authenticates
 */
class NfcReaderCallback(
    private val onRequestRead: (NfcPaymentRequest) -> Unit,
    private val onError: (String) -> Unit,
) : NfcAdapter.ReaderCallback {

    @Volatile
    private var isoDep: IsoDep? = null

    override fun onTagDiscovered(tag: Tag) {
        val iso = IsoDep.get(tag)
        if (iso == null) {
            onError("Device does not support IsoDep")
            return
        }

        try {
            iso.connect()
            iso.timeout = 30_000 // 30s — gives user time to authenticate

            // Step 1: SELECT AID → get payment request
            val selectApdu = NfcPayloadCodec.buildSelectAidApdu()
            val response = iso.transceive(selectApdu)

            val request = NfcPayloadCodec.decodeRequest(response)
            if (request == null) {
                onError("Invalid payment data from receiver")
                iso.close()
                return
            }

            // Keep connection alive for the confirm step
            isoDep = iso
            onRequestRead(request)
        } catch (e: IOException) {
            onError("NFC connection failed: ${e.message}")
            runCatching { iso.close() }
        }
    }

    /**
     * Send payment confirmation back to the receiver's HCE.
     * Must be called from a background thread.
     *
     * @return true if the receiver acknowledged the confirmation
     */
    fun sendConfirmation(confirmation: NfcPaymentConfirmation): Boolean {
        val iso = isoDep ?: return false
        return try {
            if (!iso.isConnected) return false
            val confirmApdu = NfcPayloadCodec.buildConfirmApdu(confirmation)
            val response = iso.transceive(confirmApdu)
            NfcPayloadCodec.isSuccess(response)
        } catch (e: IOException) {
            false
        } finally {
            runCatching { iso.close() }
            isoDep = null
        }
    }

    fun close() {
        runCatching { isoDep?.close() }
        isoDep = null
    }
}
