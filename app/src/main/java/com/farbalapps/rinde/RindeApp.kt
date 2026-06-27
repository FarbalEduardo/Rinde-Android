package com.farbalapps.rinde

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit
import com.farbalapps.rinde.data.worker.FeedSyncWorker

@HiltAndroidApp
class RindeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("RindeApp", "🚀 Rinde inicializada (Cloudinary via REST API)")
        
        // Inicializar Firebase App Check
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                    com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                )
                android.util.Log.i("RindeApp", "🔒 Firebase App Check configurado con DEBUG provider")
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                    com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                android.util.Log.i("RindeApp", "🔒 Firebase App Check configurado con PLAY INTEGRITY provider")
            }
        } catch (e: Exception) {
            android.util.Log.e("RindeApp", "❌ Error al inicializar Firebase App Check: ${e.message}", e)
        }

        // Programar sincronización periódica del feed
        scheduleFeedSync()
    }

    private fun scheduleFeedSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<FeedSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "feed_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        android.util.Log.i("RindeApp", "⏰ FeedSyncWorker periódico registrado exitosamente")
    }
}
