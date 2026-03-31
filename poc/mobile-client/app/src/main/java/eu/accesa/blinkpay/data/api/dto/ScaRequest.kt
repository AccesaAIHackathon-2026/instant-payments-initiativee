package eu.accesa.blinkpay.data.api.dto

data class ScaRequest(
    val uetr: String,
    val scaChallengeToken: String,
    val pin: String = "1234",
)
