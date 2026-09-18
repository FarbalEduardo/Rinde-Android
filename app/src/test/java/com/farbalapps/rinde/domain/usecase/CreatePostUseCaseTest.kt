package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.moderation.ContentModerator
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
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
 * [HU-01] Pruebas unitarias para la publicación de ofertas en la comunidad.
 * Valida validación de campos obligatorios, moderación de contenido y orquestación de subida.
 */
class CreatePostUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var moderationExpert: ContentModerator
    private lateinit var createPostUseCase: CreatePostUseCase

    @Before
    fun setUp() {
        feedRepository = mockk(relaxed = true)
        authRepository = mockk()
        moderationExpert = mockk()

        every { authRepository.getCurrentUser() } returns User(
            id = "user_123",
            name = "Test User",
            email = "test@example.com"
        )

        createPostUseCase = CreatePostUseCase(
            feedRepository = feedRepository,
            authRepository = authRepository,
            moderationExpert = moderationExpert
        )
    }

    /**
     * [HU-01] Título vacío debe ser rechazado antes de invocar servicios externos.
     */
    @Test
    fun `cuando el titulo esta vacio debe fallar con mensaje claro`() = runBlocking {
        val result = createPostUseCase(
            title = "   ",
            description = "Descripción válida",
            category = "Tecnología",
            locationName = "Lima",
            photos = emptyList()
        )

        assertTrue(result.isFailure)
        assertEquals("El título es obligatorio", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { feedRepository.uploadPostWithImages(any(), any()) }
    }

    /**
     * [HU-01] Descripción vacía debe ser rechazada inmediatamente.
     */
    @Test
    fun `cuando la descripcion esta vacia debe fallar`() = runBlocking {
        val result = createPostUseCase(
            title = "Título válido",
            description = "",
            category = "Tecnología",
            locationName = "Lima",
            photos = emptyList()
        )

        assertTrue(result.isFailure)
        assertEquals("La descripción es obligatoria", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { feedRepository.uploadPostWithImages(any(), any()) }
    }

    /**
     * [HU-01] Si el filtro de moderación detecta contenido prohibido, el post es rechazado.
     */
    @Test
    fun `cuando la moderacion rechaza el texto debe fallar con la razon`() = runBlocking {
        every { moderationExpert.analyzeText(any(), any()) } returns ContentModerator.ModerationResult.Rejected("Lenguaje ofensivo")

        val result = createPostUseCase(
            title = "Oferta sospechosa",
            description = "Texto no permitido",
            category = "Varios",
            locationName = "Online",
            photos = emptyList()
        )

        assertTrue(result.isFailure)
        assertEquals("Lenguaje ofensivo", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { feedRepository.uploadPostWithImages(any(), any()) }
    }

    /**
     * [HU-01] Flujo exitoso: texto aprobado por moderación, post subido correctamente con fotos en formato String.
     */
    @Test
    fun `cuando los datos son validos y moderacion aprueba debe publicar con exito`() = runBlocking {
        every { moderationExpert.analyzeText(any(), any()) } returns ContentModerator.ModerationResult.Approved
        coEvery { feedRepository.uploadPostWithImages(any(), any()) } returns Result.success(Unit)

        val photos = listOf("file://photo1.jpg", "file://photo2.jpg")
        val result = createPostUseCase(
            title = "Oferta PlayStation 5",
            description = "PS5 Slim a excelente precio en tienda",
            category = "Videojuegos",
            locationName = "Saga Falabella",
            photos = photos,
            offerType = OfferType.PHYSICAL,
            normalPrice = 2500.0,
            discountPrice = 1999.0
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { feedRepository.uploadPostWithImages(any(), photos) }
    }
}
