package eu.accesa.blinkpay.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.Gson
import eu.accesa.blinkpay.data.model.QrPaymentData
import eu.accesa.blinkpay.ui.home.HomeScreen
import eu.accesa.blinkpay.ui.lock.BiometricLockScreen
import eu.accesa.blinkpay.ui.payment.PaymentConfirmScreen
import eu.accesa.blinkpay.ui.payment.PaymentResultScreen
import eu.accesa.blinkpay.ui.qr.QrScanScreen

object Routes {
    const val LOCK = "lock"
    const val HOME = "home"
    const val QR_SCAN = "qr_scan"
    const val PAYMENT_CONFIRM = "payment_confirm/{qrDataJson}"
    const val PAYMENT_RESULT = "payment_result/{success}/{message}?uetr={uetr}"

    fun paymentConfirm(qrData: QrPaymentData): String {
        val json = Uri.encode(Gson().toJson(qrData))
        return "payment_confirm/$json"
    }

    fun paymentResult(success: Boolean, message: String, uetr: String? = null): String {
        val encodedMessage = Uri.encode(message)
        return "payment_result/$success/$encodedMessage?uetr=${uetr ?: ""}"
    }
}

@Composable
fun BlinkPayNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOCK,
    ) {
        composable(Routes.LOCK) {
            BiometricLockScreen(
                onUnlocked = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onScanQr = {
                    navController.navigate(Routes.QR_SCAN)
                }
            )
        }

        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onQrScanned = { qrData ->
                    navController.navigate(Routes.paymentConfirm(qrData)) {
                        popUpTo(Routes.QR_SCAN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PAYMENT_CONFIRM,
            arguments = listOf(navArgument("qrDataJson") { type = NavType.StringType }),
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("qrDataJson") ?: ""
            val qrData = remember(json) { Gson().fromJson(json, QrPaymentData::class.java) }

            PaymentConfirmScreen(
                qrData = qrData,
                onPaymentComplete = { success, uetr, message ->
                    navController.navigate(Routes.paymentResult(success, message, uetr)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PAYMENT_RESULT,
            arguments = listOf(
                navArgument("success") { type = NavType.BoolType },
                navArgument("message") { type = NavType.StringType },
                navArgument("uetr") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val success = backStackEntry.arguments?.getBoolean("success") ?: false
            val message = backStackEntry.arguments?.getString("message") ?: ""
            val uetr = backStackEntry.arguments?.getString("uetr")?.ifEmpty { null }

            PaymentResultScreen(
                success = success,
                uetr = uetr,
                message = message,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
