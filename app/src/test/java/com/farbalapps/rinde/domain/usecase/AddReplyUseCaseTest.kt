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

/**
 * [HU-03] Pruebas unitarias para respuestas a comentarios en ofertas.
 * Valida menciones de usuario, autenticación y manejo de imágenes adjuntas.
 */
class AddReplyUseCaseTest {

    private lateinit var commentRepository: CommentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var addReplyUseCase: AddReplyUseCase

    @Before
    fun setUp() {
        commentRepository = mockk()
        authRepository = mockk()
        addReplyUseCase = AddReplyUseCase(commentRepository, authRepository)
    }

    /**
     * [HU-03] Usuario no autenticado no puede responder.
     */
    @Test
    fun `cuando el usuario no esta autenticado debe fallar`() = runBlocking {
        every { authRepository.getCurrentUser() } returns null

        val result = addReplyUseCase(
            commentId = "comm_1",
            postId = "post_1",
            text = "Sí, es gratis con Prime"
        )

        assertTrue(result.isFailure)
        assertEquals("Usuario no autenticado", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { commentRepository.addReply(any(), any(), any()) }
    }

    /**
     * [HU-03] Respuesta con mención a otro usuario.
     */
    @Test
    fun `cuando responde con mencion debe persistir mentionedUser`() = runBlocking {
        every { authRepository.getCurrentUser() } returns User(
            id = "user_789",
            displayName = "Pedro Sánchez",
            email = "pedro@test.com"
        )
        coEvery { commentRepository.addReply(any(), any(), any()) } returns Result.success(Unit)

        val result = addReplyUseCase(
            commentId = "comm_1",
            postId = "post_1",
            text = "Totalmente de acuerdo contigo",
            mentionedUser = "Ana Gómez",
            imageUri = null
        )

        assertTrue(result.isSuccess)
        val reply = result.getOrNull()
        assertEquals("Ana Gómez", reply?.mentionedUser)
        assertEquals("user_789", reply?.authorId)
        coVerify(exactly = 1) { commentRepository.addReply("comm_1", any(), null) }
    }
}
