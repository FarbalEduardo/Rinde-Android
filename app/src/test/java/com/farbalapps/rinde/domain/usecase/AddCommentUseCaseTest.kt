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
 * [HU-03] Pruebas unitarias para publicación de comentarios en ofertas.
 * Valida autenticación del usuario, subida de comentarios con y sin imagen.
 */
class AddCommentUseCaseTest {

    private lateinit var commentRepository: CommentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var addCommentUseCase: AddCommentUseCase

    @Before
    fun setUp() {
        commentRepository = mockk()
        authRepository = mockk()
        addCommentUseCase = AddCommentUseCase(commentRepository, authRepository)
    }

    /**
     * [HU-03] Usuario no autenticado no puede comentar y recibe error descriptivo.
     */
    @Test
    fun `cuando el usuario no esta autenticado debe retornar failure`() = runBlocking {
        every { authRepository.getCurrentUser() } returns null

        val result = addCommentUseCase(postId = "post_1", text = "Buen precio")

        assertTrue(result.isFailure)
        assertEquals("Usuario no autenticado", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { commentRepository.addComment(any(), any(), any()) }
    }

    /**
     * [HU-03] Comentario exitoso sin imagen asociada.
     */
    @Test
    fun `cuando el usuario esta autenticado debe agregar comentario exitosamente`() = runBlocking {
        every { authRepository.getCurrentUser() } returns User(
            id = "user_456",
            displayName = "Ana Gómez",
            email = "ana@test.com"
        )
        coEvery { commentRepository.addComment(any(), any(), any()) } returns Result.success(Unit)

        val result = addCommentUseCase(
            postId = "post_1",
            text = "¿Alguien sabe si el envío es gratis?",
            imageUri = null
        )

        assertTrue(result.isSuccess)
        assertEquals("user_456", result.getOrNull()?.authorId)
        assertEquals("¿Alguien sabe si el envío es gratis?", result.getOrNull()?.text)
        coVerify(exactly = 1) { commentRepository.addComment("post_1", any(), null) }
    }

    /**
     * [HU-03] Comentario con imagen adjunta (URI en formato String pura).
     */
    @Test
    fun `cuando se incluye imagen debe enviarla como String al repositorio`() = runBlocking {
        every { authRepository.getCurrentUser() } returns User(
            id = "user_456",
            displayName = "Ana Gómez",
            email = "ana@test.com"
        )
        coEvery { commentRepository.addComment(any(), any(), any()) } returns Result.success(Unit)

        val result = addCommentUseCase(
            postId = "post_1",
            text = "Aquí está la foto del ticket",
            imageUri = "content://media/photos/ticket.jpg"
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            commentRepository.addComment("post_1", any(), "content://media/photos/ticket.jpg")
        }
    }
}
