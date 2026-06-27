package com.farbalapps.rinde.ui.screen.home.community

import android.net.Uri
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.entity.CommunityPostEntity
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.domain.model.*
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.util.LocationService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias del flujo de fotos en EditPostViewModel.
 *
 * Escenarios cubiertos:
 *  A) Sin cambios de foto → se conservan las remotas
 *  B) Agregar 1 foto nueva a las 3 existentes → total 4 (remotas + nueva)
 *  C) Quitar 1 foto remota → queda solo en remotePhotoUrls reducidas
 *  D) Quitar 1 foto nueva → queda solo en newPhotoUris reducidas
 *  E) Respetar límite de 4 fotos totales (3 remotas + intentar 3 nuevas = solo 1 nueva aceptada)
 *  F) Post sin fotos → error de validación al guardar
 *  G) El repositorio recibe oldPhotoUrls = remotas sin las eliminadas + newPhotoUris = las nuevas
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditPostViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val feedRepository = mockk<FeedRepository>()
    private val postDao = mockk<PostDao>()
    private val locationService = mockk<LocationService>()

    private lateinit var viewModel: EditPostViewModel

    // Datos de prueba
    private val remoteUrl1 = "https://res.cloudinary.com/rinde/image/upload/foto1.jpg"
    private val remoteUrl2 = "https://res.cloudinary.com/rinde/image/upload/foto2.jpg"
    private val remoteUrl3 = "https://res.cloudinary.com/rinde/image/upload/foto3.jpg"

    private lateinit var newUri1: Uri
    private lateinit var newUri2: Uri
    private lateinit var newUri3: Uri

    private val testPostEntity = mockk<CommunityPostEntity>()
    private val testPost = CommunityPost(
        id = "post_001",
        authorId = "user_001",
        authorName = "Test Author",
        authorPhotoUrl = null,
        timestamp = null,
        title = "Test Post",
        descriptionShort = "Short desc",
        descriptionLong = "Long description for testing",
        photos = listOf(remoteUrl1, remoteUrl2, remoteUrl3),
        category = "Electrónica",
        location = PostLocation("CDMX", null, null),
        isActive = true,
        likesCount = 0,
        commentsCount = 0,
        truthCount = 0,
        falseCount = 0,
        votesScore = 0,
        verificationStatus = VerificationStatus.PENDING,
        reportCount = 0,
        userReputationScore = 0f,
        isAuthorVerified = false,
        offerType = OfferType.ONLINE,
        websiteName = "amazon.com",
        productLink = "https://amazon.com/prod",
        storeName = null,
        isRecommended = false,
        expiresAt = null,
        normalPrice = 1000.0,
        discountPrice = 800.0,
        currency = "MXN",
        couponCode = null,
        discountPercentage = 20,
        isAvailable = true,
        condition = "Nuevo"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val uriStr = firstArg<String>()
            val mockUri = mockk<Uri>()
            every { mockUri.toString() } returns uriStr
            mockUri
        }

        newUri1 = Uri.parse("content://media/external/images/media/101")
        newUri2 = Uri.parse("content://media/external/images/media/102")
        newUri3 = Uri.parse("content://media/external/images/media/103")

        // Inicialmente el postDao devuelve nuestro post de prueba
        coEvery { postDao.getPostById("post_001") } returns testPostEntity
        mockkStatic("com.farbalapps.rinde.data.local.entity.CommunityPostEntityKt")
        every { testPostEntity.toDomainModel() } returns testPost

        viewModel = EditPostViewModel(feedRepository, postDao, locationService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Uri::class)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun loadPost() {
        viewModel.loadPost("post_001")
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private val state get() = viewModel.uiState.value

    // ─── Escenario A: Sin cambio de fotos ────────────────────────────────────

    @Test
    fun `A - sin cambio de fotos, remotePhotoUrls conserva todas las originales`() {
        loadPost()

        assertEquals(
            listOf(remoteUrl1, remoteUrl2, remoteUrl3),
            state.remotePhotoUrls
        )
        assertTrue("No debe haber fotos nuevas", state.newPhotoUris.isEmpty())
    }

    @Test
    fun `A - sin cambio de fotos, submitChanges pasa oldPhotoUrls con las 3 remotas y newPhotoUris vacio`() = runTest {
        loadPost()

        coEvery {
            feedRepository.updatePost(
                postId = any(), title = any(), descriptionLong = any(), category = any(),
                normalPrice = any(), discountPrice = any(), currency = any(),
                couponCode = any(), discountPercentage = any(), isAvailable = any(),
                condition = any(), websiteName = any(), productLink = any(),
                storeName = any(), locationName = any(),
                oldPhotoUrls = any(),
                newPhotoUris = any()
            )
        } returns Result.success(Unit)

        viewModel.submitChanges()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            feedRepository.updatePost(
                postId = "post_001",
                oldPhotoUrls = listOf(remoteUrl1, remoteUrl2, remoteUrl3),
                newPhotoUris = emptyList(),
                title = any(), descriptionLong = any(), category = any(),
                normalPrice = any(), discountPrice = any(), currency = any(),
                couponCode = any(), discountPercentage = any(), isAvailable = any(),
                condition = any(), websiteName = any(), productLink = any(),
                storeName = any(), locationName = any()
            )
        }
    }

    // ─── Escenario B: Agregar 1 foto nueva a las 3 existentes ────────────────

    @Test
    fun `B - agregar 1 foto nueva mantiene 3 remotas y agrega la nueva`() {
        loadPost()

        viewModel.onPhotosSelected(listOf(newUri1))

        assertEquals(
            "Remotas deben seguir siendo 3",
            listOf(remoteUrl1, remoteUrl2, remoteUrl3),
            state.remotePhotoUrls
        )
        assertEquals(
            "Debe haber 1 foto nueva",
            listOf(newUri1),
            state.newPhotoUris
        )
    }

    @Test
    fun `B - submitChanges con 1 nueva pasa oldPhotoUrls con 3 y newPhotoUris con 1`() = runTest {
        loadPost()
        viewModel.onPhotosSelected(listOf(newUri1))

        coEvery {
            feedRepository.updatePost(
                postId = any(), title = any(), descriptionLong = any(), category = any(),
                normalPrice = any(), discountPrice = any(), currency = any(),
                couponCode = any(), discountPercentage = any(), isAvailable = any(),
                condition = any(), websiteName = any(), productLink = any(),
                storeName = any(), locationName = any(),
                oldPhotoUrls = any(),
                newPhotoUris = any()
            )
        } returns Result.success(Unit)

        viewModel.submitChanges()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            feedRepository.updatePost(
                postId = "post_001",
                oldPhotoUrls = listOf(remoteUrl1, remoteUrl2, remoteUrl3),
                newPhotoUris = listOf("content://media/external/images/media/101"),
                title = any(), descriptionLong = any(), category = any(),
                normalPrice = any(), discountPrice = any(), currency = any(),
                couponCode = any(), discountPercentage = any(), isAvailable = any(),
                condition = any(), websiteName = any(), productLink = any(),
                storeName = any(), locationName = any()
            )
        }
    }

    // ─── Escenario C: Quitar una foto remota ─────────────────────────────────

    @Test
    fun `C - quitar foto remota la elimina solo de remotePhotoUrls`() {
        loadPost()

        viewModel.onPhotoRemoved(Uri.parse(remoteUrl2))

        assertEquals(
            "Deben quedar solo remoteUrl1 y remoteUrl3",
            listOf(remoteUrl1, remoteUrl3),
            state.remotePhotoUrls
        )
        assertTrue("newPhotoUris sigue vacío", state.newPhotoUris.isEmpty())
    }

    // ─── Escenario D: Quitar una foto nueva ──────────────────────────────────

    @Test
    fun `D - quitar foto nueva la elimina solo de newPhotoUris`() {
        loadPost()

        viewModel.onPhotosSelected(listOf(newUri1))
        viewModel.onPhotoRemoved(newUri1)

        assertEquals(
            "Remotas no deben haber cambiado",
            listOf(remoteUrl1, remoteUrl2, remoteUrl3),
            state.remotePhotoUrls
        )
        assertTrue("newPhotoUris debe estar vacío", state.newPhotoUris.isEmpty())
    }

    // ─── Escenario E: Límite de 4 fotos ──────────────────────────────────────

    @Test
    fun `E - con 3 remotas solo se acepta 1 foto nueva (limite total 4)`() {
        loadPost()

        // Intenta agregar 3 fotos nuevas, pero hay 3 remotas → solo cabe 1
        viewModel.onPhotosSelected(listOf(newUri1, newUri2, newUri3))

        assertEquals(
            "Solo debe aceptar 1 foto nueva para no pasar de 4 en total",
            1,
            state.newPhotoUris.size
        )
        assertEquals(newUri1, state.newPhotoUris.first())
    }

    @Test
    fun `E - con 0 remotas acepta hasta 4 fotos nuevas`() {
        // Post sin fotos
        val emptyPhotosPost = testPost.copy(photos = emptyList())
        every { testPostEntity.toDomainModel() } returns emptyPhotosPost
        loadPost()

        viewModel.onPhotosSelected(listOf(newUri1, newUri2, newUri3))

        assertEquals(
            "Debe aceptar las 3 fotos nuevas con 0 remotas",
            3,
            state.newPhotoUris.size
        )
    }

    // ─── Escenario F: Validación — sin fotos al guardar ──────────────────────

    @Test
    fun `F - no puede guardar si todas las fotos fueron eliminadas`() = runTest {
        loadPost()

        // Eliminar todas las remotas
        viewModel.onPhotoRemoved(Uri.parse(remoteUrl1))
        viewModel.onPhotoRemoved(Uri.parse(remoteUrl2))
        viewModel.onPhotoRemoved(Uri.parse(remoteUrl3))

        viewModel.submitChanges()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Debe haber un error de validación", state.error)
        assertTrue(
            "El error debe mencionar imagen",
            state.error!!.contains("imagen", ignoreCase = true)
        )
        // El repositorio NO debe ser llamado
        coVerify(exactly = 0) { feedRepository.updatePost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // ─── Escenario G: No agregar duplicados ──────────────────────────────────

    @Test
    fun `G - seleccionar la misma URI dos veces no crea duplicados`() {
        loadPost()

        viewModel.onPhotosSelected(listOf(newUri1))
        viewModel.onPhotosSelected(listOf(newUri1)) // misma URI de nuevo

        assertEquals(
            "No debe haber duplicados en newPhotoUris",
            1,
            state.newPhotoUris.size
        )
    }
}
