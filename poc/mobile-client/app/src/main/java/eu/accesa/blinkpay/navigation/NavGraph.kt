package eu.accesa.blinkpay.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import eu.accesa.blinkpay.ui.home.HomeScreen
import eu.accesa.blinkpay.ui.lock.BiometricLockScreen

object Routes {
    const val LOCK = "lock"
    const val HOME = "home"
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
            HomeScreen()
        }
    }
}
