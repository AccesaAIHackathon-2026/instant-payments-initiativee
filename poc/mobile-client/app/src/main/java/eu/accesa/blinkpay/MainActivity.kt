package eu.accesa.blinkpay

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import eu.accesa.blinkpay.navigation.BlinkPayNavGraph
import eu.accesa.blinkpay.navigation.Routes
import eu.accesa.blinkpay.ui.theme.BlinkPayTheme
import eu.accesa.blinkpay.util.ServiceLocator
import eu.accesa.blinkpay.util.UserSession

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.init(applicationContext)
        enableEdgeToEdge()

        setContent {
            val appTheme by ServiceLocator.themeState.collectAsState()
            val isLocked by ServiceLocator.isLocked.collectAsState()

            BlinkPayTheme(appTheme = appTheme) {
                val navController = rememberNavController()

                // When the app returns from background, navigate to lock screen so the
                // user re-enters their PIN. Skip if no session exists yet (first launch).
                androidx.compose.runtime.LaunchedEffect(isLocked) {
                    if (isLocked && UserSession.isRegistered) {
                        navController.navigate(Routes.LOCK) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                BlinkPayNavGraph(navController = navController)
            }
        }
    }
}
