package com.farbalapps.rinde.data.logger

import android.util.Log
import com.farbalapps.rinde.util.logger.AppLogger
import javax.inject.Inject

/**
 * Implementación Android de [AppLogger] usando [android.util.Log].
 *
 * Esta clase vive en la capa `data/` ya que depende del SDK de Android.
 * La interfaz [AppLogger] vive en `util/` (Kotlin puro), permitiendo
 * que ViewModels y tests usen la abstracción sin conocer esta implementación.
 *
 * Provista como @Singleton mediante [com.farbalapps.rinde.di.LoggingModule].
 */
class AndroidAppLogger @Inject constructor() : AppLogger {

    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    override fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }
}
