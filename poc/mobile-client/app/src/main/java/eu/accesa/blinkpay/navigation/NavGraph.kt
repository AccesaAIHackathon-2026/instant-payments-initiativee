package eu.accesa.blinkpay.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import eu.accesa.blinkpay.data.model.QrPaymentData
import eu.accesa.blinkpay.ui.account.AccountScreen
import eu.accesa.blinkpay.ui.home.HomeScreen
import eu.accesa.blinkpay.ui.lock.BiometricLockScreen
import eu.accesa.blinkpay.ui.payment.PaymentConfirmScreen
import eu.accesa.blinkpay.ui.payment.PaymentResultScreen
import eu.accesa.blinkpay.ui.payment.PaymentViewModel
import eu.accesa.blinkpay.ui.qr.QrScanScreen
import eu.accesa.blinkpay.ui.registration.RegistrationScreen
import eu.accesa.blinkpay.util.UserSession
import java.math.BigDecimal
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val REGISTER = "register"
    const val LOCK = "lock"
    const val HOME = "home"
    const val QR_SCAN = "qr_scan"
    const val ACCOUNT = "account"

    // Payment confirm route with encoded QR data as arguments
    const val PAYMENT_CONFIRM =
        "payment_confirm/{creditorName}/{creditorIban}/{amount}/{currency}/{creditorReference}/{reference}"

    fun paymentConfirm(p: QrPaymentData): String {
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        return "payment_confirm/${enc(p.creditorName)}/${enc(p.creditorIban)}" +
                "/${p.amount.toPlainString()}/${p.currency}/${enc(p.creditorReference ?: "")}/${enc(p.reference)}"
    }

    // Payment result route
    const val PAYMENT_RESULT = "payment_result/{success}/{uetr}/{reason}"

    fun paymentResult(success: Boolean, uetr: String?, reason: String?): String {
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        return "payment_result/$success/${enc(uetr ?: "")}/${enc(reason ?: "")}"
    }
}

@Composable
fun BlinkPayNavGraph(navController: NavHostController) {
    val startDestination = if (UserSession.isRegistered) Routes.LOCK else Routes.REGISTER

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.REGISTER) {
            RegistrationScreen(
                onRegistered = {
                    navController.navigate(Routes.LOCK) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

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
                onScanAndPay = { navController.navigate(Routes.QR_SCAN) },
                onAccount = { navController.navigate(Routes.ACCOUNT) },
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onScanned = { payment ->
                    navController.navigate(Routes.paymentConfirm(payment)) {
                        popUpTo(Routes.QR_SCAN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PAYMENT_CONFIRM,
            arguments = listOf(
                navArgument("creditorName") { type = NavType.StringType },
                navArgument("creditorIban") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType },
                navArgument("creditorReference") { type = NavType.StringType },
                navArgument("reference") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val dec = { key: String -> URLDecoder.decode(args.getString(key)!!, "UTF-8") }

            val payment = remember {
                QrPaymentData(
                    creditorName = dec("creditorName"),
                    creditorIban = dec("creditorIban"),
                    amount = BigDecimal(args.getString("amount")!!),
                    currency = dec("currency"),
                    creditorReference = dec("creditorReference").ifBlank { null },
                    reference = dec("reference"),
                )
            }

            val paymentViewModel: PaymentViewModel = viewModel()
            remember { paymentViewModel.setPayment(payment); true }

            PaymentConfirmScreen(
                payment = payment,
                viewModel = paymentViewModel,
                onResult = { success, uetr, reason ->
                    navController.navigate(Routes.paymentResult(success, uetr, reason)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onCancel = { navController.popBackStack(Routes.HOME, false) },
            )
        }

        composable(
            route = Routes.PAYMENT_RESULT,
            arguments = listOf(
                navArgument("success") { type = NavType.BoolType },
                navArgument("uetr") { type = NavType.StringType },
                navArgument("reason") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val dec = { key: String -> URLDecoder.decode(args.getString(key)!!, "UTF-8") }

            PaymentResultScreen(
                success = args.getBoolean("success"),
                uetr = dec("uetr").ifBlank { null },
                reason = dec("reason").ifBlank { null },
                onDone = {
                    navController.popBackStack(Routes.HOME, false)
                },
            )
        }
    }
}
