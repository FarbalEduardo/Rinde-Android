package com.farbalapps.rinde.util.logger

/**
 * Abstracción de logging desacoplada de [android.util.Log].
 *
 * Beneficios:
 * - Tests unitarios sin Robolectric ni emulador.
 * - Sustitución futura por Timber o Crashlytics sin tocar el código de producción.
 * - ViewModels y UseCases no dependen del SDK de Android.
 *
 * Provista vía Hilt como @Singleton desde [com.farbalapps.rinde.di.LoggingModule].
 */
interface AppLogger {
    /**
     * Registra un mensaje de nivel DEBUG.
     * @param tag Identificador de la clase o componente que emite el log.
     * @param message Mensaje descriptivo de la operación.
     */
    fun debug(tag: String, message: String)

    /**
     * Registra un mensaje de nivel ERROR.
     * @param tag Identificador de la clase o componente que emite el log.
     * @param message Descripción del error ocurrido.
     * @param throwable Excepción opcional asociada al error.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Registra un mensaje de nivel WARN.
     * @param tag Identificador de la clase o componente que emite el log.
     * @param message Mensaje de advertencia.
     */
    fun warn(tag: String, message: String)
}
