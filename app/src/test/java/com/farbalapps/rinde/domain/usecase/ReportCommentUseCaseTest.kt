package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CommentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportCommentUseCaseTest {

    private lateinit var commentRepository: CommentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var reportCommentUseCase: ReportCommentUseCase

    @Before
    fun setUp() {
        commentRepository = mockk()
        authRepository = mockk()
        reportCommentUseCase = ReportCommentUseCase(
            commentRepository,
            authRepository
        )
    }

    @Test
    fun `cuando el usuario no esta autenticado debe fallar`() = runBlocking {
        // Arrange
        every { authRepository.getCurrentUser() } returns null

        // Act
        val result = reportCommentUseCase("post123", "comment123", "Comentario inapropiado", "author456")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Usuario no autenticado", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { commentRepository.reportComment(any()) }
    }

    @Test
    fun `cuando el repo falla debe devolver error`() = runBlocking {
        // Arrange
        val userId = "user123"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com", displayName = "Reporter")
        coEvery { commentRepository.reportComment(any()) } returns Result.failure(Exception("Firestore Error"))

        // Act
        val result = reportCommentUseCase("post123", "comment123", "Comentario inapropiado", "author456")

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Firestore Error", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { commentRepository.reportComment(any()) }
    }

    @Test
    fun `cuando el reporte es exitoso debe devolver success`() = runBlocking {
        // Arrange
        val userId = "user123"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com", displayName = "Reporter")
        coEvery { commentRepository.reportComment(any()) } returns Result.success(Unit)

        // Act
        val result = reportCommentUseCase("post123", "comment123", "Comentario inapropiado", "author456")

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { commentRepository.reportComment(any()) }
    }
}
