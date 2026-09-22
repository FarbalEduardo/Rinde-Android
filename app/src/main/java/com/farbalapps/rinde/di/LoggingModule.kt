package com.farbalapps.rinde.di

import com.farbalapps.rinde.data.logger.AndroidAppLogger
import com.farbalapps.rinde.util.logger.AppLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt que vincula la interfaz [AppLogger] con su implementación concreta [AndroidAppLogger].
 *
 * Al usar @Binds en lugar de @Provides, Hilt no necesita instanciar el módulo
 * (es abstract) lo que es más eficiente en tiempo de compilación.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    /**
     * Vincula [AndroidAppLogger] como la implementación singleton de [AppLogger].
     * Toda la app recibe la misma instancia vía inyección de dependencias.
     */
    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: AndroidAppLogger): AppLogger
}
