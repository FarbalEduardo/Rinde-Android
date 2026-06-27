package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.community.UpdateAuthorTrustScoreUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleVoteUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var updateAuthorTrustScoreUseCase: UpdateAuthorTrustScoreUseCase
    private lateinit var toggleVoteUseCase: ToggleVoteUseCase

    @Before
    fun setUp() {
        feedRepository = mockk()
        authRepository = mockk()
        updateAuthorTrustScoreUseCase = mockk(relaxed = true)
        toggleVoteUseCase = ToggleVoteUseCase(
            feedRepository,
            authRepository,
            updateAuthorTrustScoreUseCase
        )
    }

    @Test
    fun `cuando el usuario no esta autenticado debe fallar`() = runBlocking {
        // Arrange
        every { authRepository.getCurrentUser() } returns null

        // Act
        val result = toggleVoteUseCase("post123", 1, "author456")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Usuario no autenticado", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { feedRepository.toggleVote(any(), any(), any()) }
    }

    @Test
    fun `cuando el repo falla debe devolver error y no actualizar reputacion`() = runBlocking {
        // Arrange
        val userId = "user123"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.toggleVote(userId, "post123", 1) } returns Result.failure(Exception("DB Error"))

        // Act
        val result = toggleVoteUseCase("post123", 1, "author456")

        // Assert
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { updateAuthorTrustScoreUseCase(any()) }
    }

    @Test
    fun `cuando el voto es exitoso debe actualizar la reputacion del autor`() = runBlocking {
        // Arrange
        val userId = "user123"
        val authorId = "author456"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.toggleVote(userId, "post123", 1) } returns Result.success(Unit)

        // Act
        val result = toggleVoteUseCase("post123", 1, authorId)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { feedRepository.toggleVote(userId, "post123", 1) }
        coVerify(exactly = 1) { updateAuthorTrustScoreUseCase(authorId) }
    }
}
