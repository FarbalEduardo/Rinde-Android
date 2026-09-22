package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.repository.CommentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [HU-05] Pruebas unitarias para obtención reactiva de comentarios y respuestas de ofertas.
 * Valida flujos Flow reactivos en el detalle de la oferta.
 */
class GetCommentsUseCaseTest {

    private lateinit var commentRepository: CommentRepository
    private lateinit var getCommentsUseCase: GetCommentsUseCase

    @Before
    fun setUp() {
        commentRepository = mockk()
        getCommentsUseCase = GetCommentsUseCase(commentRepository)
    }

    /**
     * [HU-05] Flow emite lista de comentarios cuando existen en la oferta.
     */
    @Test
    fun `cuando hay comentarios debe emitir la lista desde el repositorio`() = runBlocking {
        val sampleComments = listOf(
            Comment(
                id = "comm_1",
                authorId = "author_1",
                authorName = "Juan",
                text = "Excelente precio",
                timestamp = 1000L
            )
        )
        every { commentRepository.getComments("post_123") } returns flowOf(sampleComments)

        val result = getCommentsUseCase.getComments("post_123").first()

        assertEquals(1, result.size)
        assertEquals("Excelente precio", result.first().text)
        verify(exactly = 1) { commentRepository.getComments("post_123") }
    }

    /**
     * [HU-05] Flow emite lista vacía si la oferta no tiene comentarios.
     */
    @Test
    fun `cuando no hay comentarios debe emitir lista vacia`() = runBlocking {
        every { commentRepository.getComments("post_empty") } returns flowOf(emptyList())

        val result = getCommentsUseCase.getComments("post_empty").first()

        assertTrue(result.isEmpty())
        verify(exactly = 1) { commentRepository.getComments("post_empty") }
    }

    /**
     * [HU-05] Obtención de respuestas anidadas a un comentario.
     */
    @Test
    fun `cuando se consultan respuestas debe emitir la lista correspondiente`() = runBlocking {
        val sampleReplies = listOf(
            Reply(
                id = "reply_1",
                commentId = "comm_1",
                postId = "post_123",
                authorId = "author_2",
                authorName = "María",
                text = "Yo también la compré",
                timestamp = 2000L
            )
        )
        every { commentRepository.getReplies("comm_1") } returns flowOf(sampleReplies)

        val result = getCommentsUseCase.getReplies("comm_1").first()

        assertEquals(1, result.size)
        assertEquals("Yo también la compré", result.first().text)
        verify(exactly = 1) { commentRepository.getReplies("comm_1") }
    }
}
