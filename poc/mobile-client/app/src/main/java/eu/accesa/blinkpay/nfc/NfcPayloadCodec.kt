package eu.accesa.blinkpay.nfc

import com.google.gson.Gson
import java.math.BigDecimal

data class NfcPaymentRequest(
    val amount: BigDecimal,
    val receiverName: String,
    val receiverIban: String,
)

data class NfcPaymentConfirmation(
    val senderName: String,
    val senderIban: String,
    val confirmed: Boolean,
)

object NfcPayloadCodec {

    private val gson = Gson()

    // Custom AID for BlinkPay NFC P2P: "BlinkPay" in hex → F0424C4E4B504159
    val AID = byteArrayOf(
        0xF0.toByte(), 0x42, 0x4C, 0x4E, 0x4B, 0x50, 0x41, 0x59
    )

    // APDU instruction byte for confirm command
    const val INS_CONFIRM: Byte = 0xC0.toByte()

    // ISO 7816 status words
    val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
    val SW_UNKNOWN = byteArrayOf(0x6F.toByte(), 0x00)
    val SW_NO_DATA = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    fun encodeRequest(request: NfcPaymentRequest): ByteArray {
        return gson.toJson(request).toByteArray(Charsets.UTF_8)
    }

    fun decodeRequest(data: ByteArray): NfcPaymentRequest? {
        return try {
            // Strip trailing status word (last 2 bytes) if present
            val json = if (data.size >= 2) {
                val payload = data.copyOf(data.size - 2)
                String(payload, Charsets.UTF_8)
            } else {
                return null
            }
            gson.fromJson(json, NfcPaymentRequest::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun encodeConfirmation(confirmation: NfcPaymentConfirmation): ByteArray {
        return gson.toJson(confirmation).toByteArray(Charsets.UTF_8)
    }

    fun decodeConfirmation(apdu: ByteArray): NfcPaymentConfirmation? {
        return try {
            // APDU: CLA INS P1 P2 Lc [data]
            // Skip the 5-byte header to get the JSON payload
            if (apdu.size <= 5) return null
            val json = String(apdu, 5, apdu.size - 5, Charsets.UTF_8)
            gson.fromJson(json, NfcPaymentConfirmation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun buildSelectAidApdu(): ByteArray {
        // CLA=0x00 INS=0xA4 P1=0x04 P2=0x00 Lc=len AID
        val header = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte())
        return header + AID
    }

    fun buildConfirmApdu(confirmation: NfcPaymentConfirmation): ByteArray {
        val payload = encodeConfirmation(confirmation)
        // CLA=0x00 INS=0xC0 P1=0x00 P2=0x00 Lc=len payload
        val header = byteArrayOf(0x00, INS_CONFIRM, 0x00, 0x00, payload.size.toByte())
        return header + payload
    }

    fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 5 + AID.size) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte()) return false
        if (apdu[2] != 0x04.toByte() || apdu[3] != 0x00.toByte()) return false
        for (i in AID.indices) {
            if (apdu[5 + i] != AID[i]) return false
        }
        return true
    }

    fun isConfirmCommand(apdu: ByteArray): Boolean {
        return apdu.size > 5 && apdu[1] == INS_CONFIRM
    }

    fun isSuccess(response: ByteArray): Boolean {
        return response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()
    }
}
