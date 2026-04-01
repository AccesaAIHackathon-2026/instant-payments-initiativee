package eu.accesa.blinkpay.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * HCE service running on the **receiver's** device.
 *
 * Protocol:
 * 1. Sender (reader) sends SELECT AID → we respond with the payment request JSON.
 * 2. Sender sends CONFIRM command (INS=0xC0) with confirmation JSON → we ACK and notify the UI.
 */
class NfcHostApduService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        return when {
            NfcPayloadCodec.isSelectAid(commandApdu) -> {
                val payload = NfcBridge.pendingRequestPayload
                if (payload != null) {
                    payload + NfcPayloadCodec.SW_OK
                } else {
                    NfcPayloadCodec.SW_NO_DATA
                }
            }

            NfcPayloadCodec.isConfirmCommand(commandApdu) -> {
                val confirmation = NfcPayloadCodec.decodeConfirmation(commandApdu)
                if (confirmation != null) {
                    NfcBridge.onConfirmationReceived(confirmation)
                    NfcPayloadCodec.SW_OK
                } else {
                    NfcPayloadCodec.SW_UNKNOWN
                }
            }

            else -> NfcPayloadCodec.SW_UNKNOWN
        }
    }

    override fun onDeactivated(reason: Int) {
        // No-op for POC
    }
}
