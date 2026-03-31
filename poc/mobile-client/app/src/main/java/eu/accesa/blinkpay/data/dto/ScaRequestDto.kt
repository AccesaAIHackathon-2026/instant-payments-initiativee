package eu.accesa.blinkpay.data.dto

data class ScaRequestDto(
    val uetr: String,
    val rtpId: String? = null,
    val pin: String,
)
