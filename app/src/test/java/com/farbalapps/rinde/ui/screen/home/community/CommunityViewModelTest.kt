package com.farbalapps.rinde.ui.screen.home.community

import app.cash.turbine.test
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import com.farbalapps.rinde.domain.usecase.VoteResult
import com.farbalapps.rinde.util.LocationService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.coroutines.cancel
import androidx.lifecycle.viewModelScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import com.farbalapps.rinde.data.local.entity.toEntity

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val toggleVoteUseCase = mockk<ToggleVoteUseCase>()
    private val locationService = mockk<LocationService>(relaxed = true)
    private val syncMetadataDao = mockk<SyncMetadataDao>(relaxed = true)
    private val postDao = mockk<PostDao>(relaxed = true)

    private lateinit var viewModel: CommunityViewModel

    private val testUserId = "user_1"
    private val testPostId = "post_123"
    private val testPost = CommunityPost(
        id = testPostId,
        authorId = "author_1",
        authorName = "Test Author",
        authorPhotoUrl = null,
        timestamp = 0L,
        title = "Test Post",
        descriptionShort = "Short",
        descriptionLong = "Long",
        photos = emptyList(),
        category = "Otros",
        location = PostLocation("Loc", null, null),
        isActive = true,
        likesCount = 0,
        commentsCount = 0,
        truthCount = 2,
        falseCount = 1,
        votesScore = 1,
        verificationStatus = VerificationStatus.PENDING,
        reportCount = 0,
        userReputationScore = 0f,
        isAuthorVerified = false,
        offerType = OfferType.PHYSICAL,
        websiteName = null,
        productLink = null,
        storeName = null,
        isRecommended = false,
        expiresAt = null,
        normalPrice = 10.0,
        discountPrice = 8.0,
        currency = "MXN",
        couponCode = null,
        discountPercentage = 20,
        isAvailable = true,
        condition = "Nuevo",
        myVoteValue = 0,
        isSavedByMe = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { authRepository.getCurrentUser() } returns mockk {
            every { id } returns testUserId
            every { displayName } returns "Test User"
        }

        every { feedRepository.globalPostStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalSavedStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalVoteStatus } returns MutableStateFlow(emptyMap())
        coEvery { postDao.getPostsOnce(any()) } returns emptyList()

        viewModel = CommunityViewModel(
            feedRepository,
            authRepository,
            toggleVoteUseCase,
            locationService,
            syncMetadataDao,
            postDao
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `setTab should update current tab in UI state`() = runTest {
        viewModel.uiState.test {
            assertEquals(CommunityTab.DISCOVER, expectMostRecentItem().currentTab)

            viewModel.setTab(CommunityTab.HOT)
            assertEquals(CommunityTab.HOT, expectMostRecentItem().currentTab)

            viewModel.setTab(CommunityTab.SAVED)
            assertEquals(CommunityTab.SAVED, expectMostRecentItem().currentTab)
        }
    }

    @Test
    fun `toggleSave should update state optimistically and rollback on failure`() = runTest {
        coEvery { feedRepository.toggleSave(testUserId, testPostId) } returns Result.failure(Exception("Network error"))

        // Set initial posts in UI state
        viewModel.setTab(CommunityTab.SAVED)
        coEvery { feedRepository.getSavedPosts(testUserId) } returns flowOf(listOf(testPost))
        viewModel.refresh()
        testScheduler.runCurrent()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertEquals(1, initialState.posts.size)
            assertEquals(false, initialState.posts.first().isSavedByMe)

            // Trigger toggleSave -> should immediately reflect saved in UI
            viewModel.toggleSave(testPostId)
            
            // Verificamos estado optimista
            val optimisticState = expectMostRecentItem()
            assertEquals(true, optimisticState.posts.first().isSavedByMe)

            // Procesar llamadas async
            testScheduler.runCurrent()

            // Verificamos rollback por fallo del servidor
            val revertedState = expectMostRecentItem()
            assertEquals(false, revertedState.posts.first().isSavedByMe)
            assertEquals("No se pudo guardar la publicación. Intenta de nuevo.", revertedState.snackbarMessage)
        }
    }

    @Test
    fun `toggleVote should update state optimistically and rollback on error`() = runTest {
        coEvery { postDao.getPostById(testPostId) } returns testPost.toEntity()
        coEvery { toggleVoteUseCase(testPostId, 1, "author_1") } returns VoteResult.ServerError("Fallo del servidor")

        // Set initial posts in UI state
        viewModel.setTab(CommunityTab.SAVED)
        coEvery { feedRepository.getSavedPosts(testUserId) } returns flowOf(listOf(testPost))
        viewModel.refresh()
        testScheduler.runCurrent()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertEquals(0, initialState.posts.first().myVoteValue)

            // Trigger toggleVote -> optimista
            viewModel.toggleVote(testPostId, 1)
            
            val optimisticState = expectMostRecentItem()
            assertEquals(1, optimisticState.posts.first().myVoteValue)

            testScheduler.runCurrent()

            // Rollback
            val revertedState = expectMostRecentItem()
            assertEquals(0, revertedState.posts.first().myVoteValue)
            assertEquals("Fallo del servidor", revertedState.snackbarMessage)
        }
    }
}
