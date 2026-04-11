package com.farbalapps.rinde

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RindeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Log if needed, usually happens if already initialized by content provider
        }
    }
}
