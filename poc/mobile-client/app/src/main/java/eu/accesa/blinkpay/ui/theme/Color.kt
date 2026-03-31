package eu.accesa.blinkpay.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// BlinkPay brand colors
private val BlinkPrimary = Color(0xFF1A73E8)
private val BlinkOnPrimary = Color.White
private val BlinkPrimaryContainer = Color(0xFFD3E3FD)
private val BlinkOnPrimaryContainer = Color(0xFF041E49)
private val BlinkSecondary = Color(0xFF00C9A7)
private val BlinkOnSecondary = Color.White
private val BlinkSecondaryContainer = Color(0xFFC4F5EC)
private val BlinkOnSecondaryContainer = Color(0xFF00382E)
private val BlinkBackground = Color(0xFFFAFAFA)
private val BlinkSurface = Color.White
private val BlinkOnBackground = Color(0xFF1A1A2E)
private val BlinkOnSurface = Color(0xFF1A1A2E)
private val BlinkError = Color(0xFFD32F2F)
private val BlinkOnError = Color.White

// BlinkPay dark colors
private val BlinkPrimaryDark = Color(0xFF8AB4F8)
private val BlinkOnPrimaryDark = Color(0xFF062E6F)
private val BlinkPrimaryContainerDark = Color(0xFF0842A0)
private val BlinkOnPrimaryContainerDark = Color(0xFFD3E3FD)
private val BlinkSecondaryDark = Color(0xFF5EECD5)
private val BlinkOnSecondaryDark = Color(0xFF00382E)
private val BlinkBackgroundDark = Color(0xFF121212)
private val BlinkSurfaceDark = Color(0xFF1E1E1E)
private val BlinkOnBackgroundDark = Color(0xFFE3E3E3)
private val BlinkOnSurfaceDark = Color(0xFFE3E3E3)

// WhiteLabel neutral colors
private val WlPrimary = Color(0xFF455A64)
private val WlOnPrimary = Color.White
private val WlPrimaryContainer = Color(0xFFCFD8DC)
private val WlOnPrimaryContainer = Color(0xFF1C313A)
private val WlSecondary = Color(0xFF26A69A)
private val WlOnSecondary = Color.White
private val WlSecondaryContainer = Color(0xFFB2DFDB)
private val WlOnSecondaryContainer = Color(0xFF00332C)
private val WlBackground = Color(0xFFFAFAFA)
private val WlSurface = Color.White
private val WlOnBackground = Color(0xFF212121)
private val WlOnSurface = Color(0xFF212121)
private val WlError = Color(0xFFD32F2F)
private val WlOnError = Color.White

// WhiteLabel dark colors
private val WlPrimaryDark = Color(0xFF90A4AE)
private val WlOnPrimaryDark = Color(0xFF1C313A)
private val WlPrimaryContainerDark = Color(0xFF37474F)
private val WlOnPrimaryContainerDark = Color(0xFFCFD8DC)
private val WlSecondaryDark = Color(0xFF80CBC4)
private val WlOnSecondaryDark = Color(0xFF00332C)
private val WlBackgroundDark = Color(0xFF121212)
private val WlSurfaceDark = Color(0xFF1E1E1E)
private val WlOnBackgroundDark = Color(0xFFE0E0E0)
private val WlOnSurfaceDark = Color(0xFFE0E0E0)

val BlinkPayLightColors = lightColorScheme(
    primary = BlinkPrimary,
    onPrimary = BlinkOnPrimary,
    primaryContainer = BlinkPrimaryContainer,
    onPrimaryContainer = BlinkOnPrimaryContainer,
    secondary = BlinkSecondary,
    onSecondary = BlinkOnSecondary,
    secondaryContainer = BlinkSecondaryContainer,
    onSecondaryContainer = BlinkOnSecondaryContainer,
    background = BlinkBackground,
    onBackground = BlinkOnBackground,
    surface = BlinkSurface,
    onSurface = BlinkOnSurface,
    error = BlinkError,
    onError = BlinkOnError,
)

val BlinkPayDarkColors = darkColorScheme(
    primary = BlinkPrimaryDark,
    onPrimary = BlinkOnPrimaryDark,
    primaryContainer = BlinkPrimaryContainerDark,
    onPrimaryContainer = BlinkOnPrimaryContainerDark,
    secondary = BlinkSecondaryDark,
    onSecondary = BlinkOnSecondaryDark,
    background = BlinkBackgroundDark,
    onBackground = BlinkOnBackgroundDark,
    surface = BlinkSurfaceDark,
    onSurface = BlinkOnSurfaceDark,
    error = BlinkError,
    onError = BlinkOnError,
)

val WhiteLabelLightColors = lightColorScheme(
    primary = WlPrimary,
    onPrimary = WlOnPrimary,
    primaryContainer = WlPrimaryContainer,
    onPrimaryContainer = WlOnPrimaryContainer,
    secondary = WlSecondary,
    onSecondary = WlOnSecondary,
    secondaryContainer = WlSecondaryContainer,
    onSecondaryContainer = WlOnSecondaryContainer,
    background = WlBackground,
    onBackground = WlOnBackground,
    surface = WlSurface,
    onSurface = WlOnSurface,
    error = WlError,
    onError = WlOnError,
)

val WhiteLabelDarkColors = darkColorScheme(
    primary = WlPrimaryDark,
    onPrimary = WlOnPrimaryDark,
    primaryContainer = WlPrimaryContainerDark,
    onPrimaryContainer = WlOnPrimaryContainerDark,
    secondary = WlSecondaryDark,
    onSecondary = WlOnSecondaryDark,
    background = WlBackgroundDark,
    onBackground = WlOnBackgroundDark,
    surface = WlSurfaceDark,
    onSurface = WlOnSurfaceDark,
    error = WlError,
    onError = WlOnError,
)
