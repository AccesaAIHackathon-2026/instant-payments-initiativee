package eu.accesa.blinkpay.util

import eu.accesa.blinkpay.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ServiceLocator {

    val themeState: MutableStateFlow<AppTheme> = MutableStateFlow(AppTheme.BLINKPAY)

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    fun lock() {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
    }
}
