package eu.accesa.blinkpay.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppTheme {
    BLINKPAY,
    WHITELABEL
}

val LocalAppTheme = staticCompositionLocalOf { AppTheme.BLINKPAY }
