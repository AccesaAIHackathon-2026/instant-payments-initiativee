package eu.accesa.blinkpay.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class BiometricAvailability {
    AVAILABLE,
    NOT_AVAILABLE
}

class BiometricHelper(private val activity: FragmentActivity) {

    fun checkAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            else -> BiometricAvailability.NOT_AVAILABLE
        }
    }

    suspend fun authenticate(
        title: String = "Authentication required",
        subtitle: String = "Verify your identity to continue",
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) continuation.resume(false)
            }

            override fun onAuthenticationFailed() {
                // Called on a single failed attempt (e.g. wrong finger).
                // The system will allow retries, so we don't resume here.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()

        prompt.authenticate(promptInfo)

        continuation.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
}
