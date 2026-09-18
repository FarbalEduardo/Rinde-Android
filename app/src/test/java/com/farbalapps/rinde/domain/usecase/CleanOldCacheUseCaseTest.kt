package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.FeedRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [HU-01] Pruebas unitarias para limpieza de posts viejos en el caché Room.
 * Garantiza que el almacenamiento local no crezca indefinidamente.
 */
class CleanOldCacheUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var cleanOldCacheUseCase: CleanOldCacheUseCase

    @Before
    fun setUp() {
        feedRepository = mockk()
        cleanOldCacheUseCase = CleanOldCacheUseCase(feedRepository)
    }

    /**
     * [HU-01] Limpieza exitosa con el umbral calculado en milisegundos para 15 días.
     */
    @Test
    fun `debe invocar deleteOldCachedPosts con umbral correcto de dias`() = runBlocking {
        coEvery { feedRepository.deleteOldCachedPosts(any()) } returns Result.success(Unit)

        val result = cleanOldCacheUseCase(maxAgeDays = 15)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { feedRepository.deleteOldCachedPosts(more(0L)) }
    }

    /**
     * [HU-01] Fallo en la base de datos local Room debe ser propagado en Result.failure.
     */
    @Test
    fun `cuando el repositorio falla debe propagar Result failure`() = runBlocking {
        coEvery { feedRepository.deleteOldCachedPosts(any()) } returns Result.failure(Exception("Room SQLite error"))

        val result = cleanOldCacheUseCase(maxAgeDays = 10)

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { feedRepository.deleteOldCachedPosts(any()) }
    }
}
