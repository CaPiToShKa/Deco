package com.example.decosocio

import android.app.Application
import com.example.decosocio.di.appModule
import com.example.decosocio.push.PushManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DecoSocioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AppConfig.fromBuildConfig()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
            androidContext(this@DecoSocioApp)
            modules(appModule(config))
        }
        // The SFMC SDK must be configured in Application.onCreate so that pushes received while
        // the app is not running (started by FCM in the background) are handled correctly.
        get<PushManager>().initialize()
    }
}
