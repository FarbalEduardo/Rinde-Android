package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.community.UpdateAuthorTrustScoreUseCase
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [HU-01] Pruebas unitarias de votación comunitaria en ofertas.
 * Valida reglas de negocio de autenticación, consistencia eventual offline y reputación.
 */
class ToggleVoteUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var updateAuthorTrustScoreUseCase: UpdateAuthorTrustScoreUseCase
    private lateinit var toggleVoteUseCase: ToggleVoteUseCase

    @Before
    fun setUp() {
        feedRepository = mockk(relaxed = true)
        authRepository = mockk()
        updateAuthorTrustScoreUseCase = mockk(relaxed = true)

        // Red disponible por defecto
        coEvery { feedRepository.isNetworkAvailable() } returns true

        toggleVoteUseCase = ToggleVoteUseCase(
            feedRepository,
            authRepository,
            updateAuthorTrustScoreUseCase
        )
    }

    /**
     * [HU-01] Escenario: Usuario anónimo intenta votar en una oferta.
     * Criterio: Debe fallar inmediatamente con error de autenticación sin tocar la base de datos.
     */
    @Test
    fun `cuando el usuario no esta autenticado debe fallar`() = runBlocking {
        every { authRepository.getCurrentUser() } returns null

        val result = toggleVoteUseCase("post123", 1, "author456")

        assertTrue(result is VoteResult.ServerError)
        assertEquals("Usuario no autenticado", (result as VoteResult.ServerError).message)
    }

    /**
     * [HU-01] Escenario: Falla la transacción remota de voto en el repositorio.
     * Criterio: Debe revertir el voto optimista local y retornar ServerError sin actualizar la reputación.
     */
    @Test
    fun `cuando el repo falla debe devolver error y no actualizar reputacion`() = runBlocking {
        val userId = "user123"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.toggleVoteTransaction(userId, "post123", 1) } returns Result.failure(Exception("DB Error"))

        val result = toggleVoteUseCase("post123", 1, "author456")

        assertTrue(result is VoteResult.ServerError)
        coVerify(exactly = 0) { updateAuthorTrustScoreUseCase(any()) }
        coVerify(exactly = 1) { feedRepository.revertOptimisticVote("post123") }
    }

    /**
     * [HU-01] Escenario: Voto positivo registrado con éxito y red activa.
     * Criterio: Debe sincronizar el voto en Room, actualizar score del post y recalcular reputación del autor.
     */
    @Test
    fun `cuando el voto es exitoso debe actualizar la reputacion del autor`() = runBlocking {
        val userId = "user123"
        val authorId = "author456"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.toggleVoteTransaction(userId, "post123", 1) } returns Result.success(Triple(5, 1, 4))
        coEvery { feedRepository.fetchPostVoteCounts("post123") } returns Result.success(Triple(5, 1, 4))

        val result = toggleVoteUseCase("post123", 1, authorId)

        assertTrue(result is VoteResult.Success)
        coVerify(exactly = 1) { feedRepository.toggleVoteTransaction(userId, "post123", 1) }
        coVerify(exactly = 1) { updateAuthorTrustScoreUseCase(authorId) }
        coVerify(exactly = 1) { feedRepository.syncLocalVoteAfterSuccess("post123", 1, Triple(5, 1, 4)) }
    }

    /**
     * [HU-01] Escenario: Usuario vota sin conexión a internet.
     * Criterio: Debe almacenar el voto en cola local Room y retornar estado Offline sin lanzar excepción.
     */
    @Test
    fun `cuando no hay red debe guardar voto offline y retornar Offline`() = runBlocking {
        val userId = "user123"
        val authorId = "author456"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.isNetworkAvailable() } returns false

        val result = toggleVoteUseCase("post123", 1, authorId)

        assertTrue(result is VoteResult.Offline)
        coVerify(exactly = 1) { feedRepository.handleOfflineVote(userId, "post123", 1, authorId) }
        coVerify(exactly = 0) { feedRepository.toggleVoteTransaction(any(), any(), any()) }
    }
}
