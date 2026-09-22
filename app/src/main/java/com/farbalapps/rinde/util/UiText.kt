package com.farbalapps.rinde.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Representa un texto orientado a la interfaz de usuario que puede ser dinámico (String plano)
 * o referenciar un recurso localizado (@StringRes) con argumentos opcionales.
 *
 * Permite que los ViewModels permanezcan completamente desacoplados del framework de Android
 * y faciliten las pruebas unitarias con JUnit y MockK sin requerir mocks de Context.
 */
sealed interface UiText {

    /**
     * Variante para cadenas de texto dinámicas (ej. mensajes del servidor o nombres).
     *
     * @param value Texto literal.
     */
    data class DynamicString(val value: String) : UiText

    /**
     * Variante para recursos de cadena en XML ([R.string.*]).
     *
     * @param resId Identificador de recurso.
     * @param args Argumentos opcionales para formato de cadena.
     */
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as StringResource
            if (resId != other.resId) return false
            if (!args.contentEquals(other.args)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    /**
     * Resuelve el texto utilizando un [Context] de Android.
     *
     * @param context Contexto de la aplicación o actividad.
     * @return Cadena de texto resuelta.
     */
    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    /**
     * Resuelve el texto de forma reactiva dentro de un Composable de Jetpack Compose.
     *
     * @return Cadena de texto resuelta.
     */
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }
}
