package com.farbalapps.rinde

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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
        
        // ──────────────────────────────────────────────────────────────────────
        // Firebase App Check
        // IMPORTANTE: NO llamar FirebaseApp.initializeApp(this) — el plugin
        // google-services lo hace automáticamente via ContentProvider ANTES de
        // que se ejecute onCreate(). Llamarlo de nuevo corrompe el estado de
        // App Check y causa "Failed to exchange debug token".
        // ──────────────────────────────────────────────────────────────────────
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            // ──────────────────────────────────────────────────────────────────────
            // Debug token leído desde local.properties via BuildConfig.
            // NUNCA hardcodear aquí — ver: firebase.google.com/docs/app-check/android/debug-provider
            // Si el token está vacío, Firebase genera uno automáticamente (visible en Logcat).
            // ──────────────────────────────────────────────────────────────────────
            val debugToken = BuildConfig.APPCHECK_DEBUG_TOKEN
            if (debugToken.isNotBlank()) {
                try {
                    val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
                    val prefsName = "com.google.firebase.appcheck.debug.store.${firebaseApp.persistenceKey}"
                    val prefs = getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", debugToken)
                        .apply()
                    android.util.Log.i("RindeApp", "🔒 App Check: Debug token inyectado desde BuildConfig")
                } catch (e: Exception) {
                    android.util.Log.w("RindeApp", "⚠️ No se pudo inyectar el debug token: ${e.message}")
                }
            } else {
                android.util.Log.w("RindeApp", "⚠️ App Check: APPCHECK_DEBUG_TOKEN no configurado en local.properties. Firebase generará uno automático (ver Logcat).")
            }

            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
                true
            )
            android.util.Log.i("RindeApp", "🔒 App Check: DEBUG provider activo.")
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
                true
            )
            android.util.Log.i("RindeApp", "🔒 App Check: PLAY INTEGRITY provider activo.")
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
