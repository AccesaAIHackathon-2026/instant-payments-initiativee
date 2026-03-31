package eu.accesa.blinkpay

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import eu.accesa.blinkpay.util.ServiceLocator

class BlinkPayApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    private class AppLifecycleObserver : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // App went to background — lock it
            ServiceLocator.lock()
        }
    }
}
