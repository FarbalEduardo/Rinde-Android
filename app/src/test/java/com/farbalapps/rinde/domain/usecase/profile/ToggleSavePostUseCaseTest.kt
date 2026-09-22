package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [HU-04] Pruebas unitarias para guardar y remover ofertas de la lista de guardados.
 * Valida persistencia y manejo de errores del repositorio.
 */
class ToggleSavePostUseCaseTest {

    private lateinit var repository: ProfileRepository
    private lateinit var useCase: ToggleSavePostUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ToggleSavePostUseCase(repository)
    }

    /**
     * [HU-04] Guardar publicación exitosamente.
     */
    @Test
    fun `cuando se solicita guardar debe llamar al repositorio con save true`() = runBlocking {
        coEvery { repository.toggleSavePost("user_1", "post_1", true) } returns Result.success(Unit)

        val result = useCase("user_1", "post_1", save = true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.toggleSavePost("user_1", "post_1", true) }
    }

    /**
     * [HU-04] Quitar publicación de guardados exitosamente.
     */
    @Test
    fun `cuando se solicita des-guardar debe llamar al repositorio con save false`() = runBlocking {
        coEvery { repository.toggleSavePost("user_1", "post_1", false) } returns Result.success(Unit)

        val result = useCase("user_1", "post_1", save = false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.toggleSavePost("user_1", "post_1", false) }
    }

    /**
     * [HU-04] Fallo al guardar debe retornar Result failure.
     */
    @Test
    fun `cuando el repositorio falla debe retornar Result failure`() = runBlocking {
        coEvery { repository.toggleSavePost(any(), any(), any()) } returns Result.failure(Exception("Network timeout"))

        val result = useCase("user_1", "post_1", true)

        assertTrue(result.isFailure)
        coVerify(exactly = 1) { repository.toggleSavePost("user_1", "post_1", true) }
    }
}
