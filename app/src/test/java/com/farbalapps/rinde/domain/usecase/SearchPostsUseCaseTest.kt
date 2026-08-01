package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.SearchResult
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.SearchRepository
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

class SearchPostsUseCaseTest {

    private lateinit var searchRepository: SearchRepository
    private lateinit var searchPostsUseCase: SearchPostsUseCase

    private val samplePost = CommunityPost(
        id = "post_1",
        authorId = "author_1",
        authorName = "Juan Pérez",
        authorPhotoUrl = null,
        timestamp = System.currentTimeMillis(),
        title = "Oferta iPhone 14",
        descriptionShort = "Descuento en tienda",
        descriptionLong = "Descripción larga de prueba",
        photos = emptyList(),
        category = "Electrónica",
        location = PostLocation("CDMX", null, null),
        isActive = true,
        likesCount = 10,
        commentsCount = 2,
        truthCount = 8,
        falseCount = 1,
        votesScore = 7,
        verificationStatus = VerificationStatus.VERIFIED,
        reportCount = 0,
        userReputationScore = 4.5f,
        isAuthorVerified = true,
        offerType = OfferType.ONLINE,
        websiteName = "Amazon",
        productLink = "https://amazon.com",
        storeName = "Amazon",
        isRecommended = true,
        expiresAt = null,
        normalPrice = 15000.0,
        discountPrice = 12000.0,
        currency = "MXN",
        couponCode = "SAVE10",
        discountPercentage = 20,
        isAvailable = true,
        condition = "Nuevo"
    )

    @Before
    fun setUp() {
        searchRepository = mockk()
        searchPostsUseCase = SearchPostsUseCase(searchRepository)
    }

    @Test
    fun `Caso 1 - Query vacia debe retornar Idle sin llamar al repositorio`() = runBlocking {
        val result = searchPostsUseCase("").first()

        assertTrue(result is SearchResult.Idle)
        verify(exactly = 0) { searchRepository.searchPosts(any(), any()) }
    }

    @Test
    fun `Caso 2 - Query de 1 solo caracter debe retornar Idle sin llamar al repositorio`() = runBlocking {
        val result = searchPostsUseCase("a").first()

        assertTrue(result is SearchResult.Idle)
        verify(exactly = 0) { searchRepository.searchPosts(any(), any()) }
    }

    @Test
    fun `Caso 3 - Query con solo espacios en blanco debe retornar Idle sin llamar al repositorio`() = runBlocking {
        val result = searchPostsUseCase("     ").first()

        assertTrue(result is SearchResult.Idle)
        verify(exactly = 0) { searchRepository.searchPosts(any(), any()) }
    }

    @Test
    fun `Caso 4 - Query de 2 caracteres validos debe consultar al repositorio`() = runBlocking {
        val query = "wa"
        every { searchRepository.searchPosts(query, "") } returns flowOf(SearchResult.Success(listOf(samplePost)))

        val result = searchPostsUseCase(query).first()

        assertTrue(result is SearchResult.Success)
        assertEquals(1, (result as SearchResult.Success).posts.size)
        verify(exactly = 1) { searchRepository.searchPosts(query, "") }
    }

    @Test
    fun `Caso 5 - Query con espacios iniciales o finales debe limpiarse (trim) antes de buscar`() = runBlocking {
        val rawQuery = "  walmart  "
        val expectedCleanQuery = "walmart"
        every { searchRepository.searchPosts(expectedCleanQuery, "") } returns flowOf(SearchResult.Success(listOf(samplePost)))

        val result = searchPostsUseCase(rawQuery).first()

        assertTrue(result is SearchResult.Success)
        verify(exactly = 1) { searchRepository.searchPosts(expectedCleanQuery, "") }
    }

    @Test
    fun `Caso 6 - Query valida con filtro de categoria debe pasar ambos parametros al repositorio`() = runBlocking {
        val query = "iphone"
        val category = "Electrónica"
        every { searchRepository.searchPosts(query, category) } returns flowOf(SearchResult.Success(listOf(samplePost)))

        val result = searchPostsUseCase(query, category).first()

        assertTrue(result is SearchResult.Success)
        assertEquals("Electrónica", (result as SearchResult.Success).posts.first().category)
        verify(exactly = 1) { searchRepository.searchPosts(query, category) }
    }

    @Test
    fun `Caso 7 - Estado de error desde el repositorio debe fluir correctamente`() = runBlocking {
        val query = "errorQuery"
        val errorMessage = "Error de conexión con la base de datos"
        every { searchRepository.searchPosts(query, "") } returns flowOf(SearchResult.Error(errorMessage))

        val result = searchPostsUseCase(query).first()

        assertTrue(result is SearchResult.Error)
        assertEquals(errorMessage, (result as SearchResult.Error).message)
        verify(exactly = 1) { searchRepository.searchPosts(query, "") }
    }
}
