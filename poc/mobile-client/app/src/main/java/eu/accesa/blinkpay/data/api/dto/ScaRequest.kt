package eu.accesa.blinkpay.data.api.dto

data class ScaRequest(
    val uetr: String? = null,
    val rtpId: String? = null,
    val pin: String = "1234",
)
