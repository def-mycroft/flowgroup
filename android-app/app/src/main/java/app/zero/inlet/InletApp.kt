package app.zero.inlet

import android.app.Application
import android.os.StrictMode
import app.zero.inlet.repo.ServiceLocator

class InletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }
        ServiceLocator.init(this)
    }
}
