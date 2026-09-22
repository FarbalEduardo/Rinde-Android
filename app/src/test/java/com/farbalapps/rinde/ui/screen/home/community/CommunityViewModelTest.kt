package com.farbalapps.rinde.ui.screen.home.community

import app.cash.turbine.test
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {

    @get:Rule
    val mainDispatcherRule = com.farbalapps.rinde.util.MainDispatcherRule()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val toggleVoteUseCase = mockk<ToggleVoteUseCase>()
    private val toggleSavePostUseCase = mockk<com.farbalapps.rinde.domain.usecase.community.ToggleSavePostUseCase>(relaxed = true)
    private val deletePostUseCase = mockk<com.farbalapps.rinde.domain.usecase.community.DeletePostUseCase>(relaxed = true)
    private val markPostExpiredUseCase = mockk<com.farbalapps.rinde.domain.usecase.community.MarkPostExpiredUseCase>(relaxed = true)
    private val reportPostExpiredUseCase = mockk<com.farbalapps.rinde.domain.usecase.community.ReportPostExpiredUseCase>(relaxed = true)
    private val locationService = mockk<LocationService>(relaxed = true)
    private val cleanOldCacheUseCase = mockk<com.farbalapps.rinde.domain.usecase.CleanOldCacheUseCase>(relaxed = true)
    private val updateFeedSeenTimestampUseCase = mockk<com.farbalapps.rinde.domain.usecase.UpdateFeedSeenTimestampUseCase>(relaxed = true)
    private val logger = mockk<com.farbalapps.rinde.util.logger.AppLogger>(relaxed = true)

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
        expiresAt = null,
        normalPrice = 10.0,
        discountPrice = 8.0,
        currency = "MXN",
        couponCode = null,
        discountPercentage = 20,
        isAvailable = true,
        condition = "Nuevo",
        myVoteValue = 0
    )

    @Before
    fun setup() {
        every { authRepository.getCurrentUser() } returns mockk {
            every { id } returns testUserId
            every { displayName } returns "Test User"
        }

        every { feedRepository.globalPostStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalSavedStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalVoteStatus } returns MutableStateFlow(emptyMap())
        coEvery { feedRepository.getUnreadNotificationsCount(any()) } returns flowOf(0)
        coEvery { feedRepository.countNewPostsSince(any()) } returns 1
        coEvery { feedRepository.syncUserVotes(any()) } returns Result.success(Unit)
        coEvery { feedRepository.syncUserSavedPosts(any()) } returns Result.success(Unit)
        coEvery { cleanOldCacheUseCase.invoke(any()) } returns Result.success(Unit)
        coEvery { updateFeedSeenTimestampUseCase.invoke() } returns Unit
        coEvery { toggleSavePostUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { deletePostUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { markPostExpiredUseCase.markExpired(any()) } returns Result.success(Unit)
        coEvery { markPostExpiredUseCase.markAvailable(any()) } returns Result.success(Unit)
        coEvery { reportPostExpiredUseCase.invoke(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        viewModel = CommunityViewModel(
            feedRepository,
            authRepository,
            toggleVoteUseCase,
            toggleSavePostUseCase,
            deletePostUseCase,
            markPostExpiredUseCase,
            reportPostExpiredUseCase,
            locationService,
            cleanOldCacheUseCase,
            updateFeedSeenTimestampUseCase,
            logger
        )
    }

    @Test
    fun `setTab should update current tab in UI state`() = runTest {
        assertEquals(CommunityTab.DISCOVER, viewModel.uiState.value.currentTab)

        viewModel.setTab(CommunityTab.HOT)
        assertEquals(CommunityTab.HOT, viewModel.uiState.value.currentTab)

        viewModel.setTab(CommunityTab.SAVED)
        assertEquals(CommunityTab.SAVED, viewModel.uiState.value.currentTab)
    }

    @Test
    fun `toggleSave should update state and rollback on failure`() = runTest {
        coEvery { toggleSavePostUseCase(testUserId, testPostId) } returns Result.failure(Exception("Network error"))

        // Set initial posts in UI state
        coEvery { feedRepository.getSavedPosts(testUserId) } returns flowOf(listOf(testPost))
        viewModel.setTab(CommunityTab.SAVED)

        assertEquals(1, viewModel.uiState.value.posts.size)
        assertEquals(false, viewModel.uiState.value.posts.first().isSavedByMe)

        // Trigger toggleSave -> should trigger rollback due to failure
        viewModel.toggleSave(testPostId)

        val revertedState = viewModel.uiState.value
        assertEquals(false, revertedState.posts.first().isSavedByMe)
        assertEquals(
            com.farbalapps.rinde.util.UiText.StringResource(com.farbalapps.rinde.R.string.community_save_error),
            revertedState.snackbarMessage
        )
    }

    @Test
    fun `toggleVote should update state and rollback on error`() = runTest {
        coEvery { toggleVoteUseCase(testPostId, 1, "author_1") } returns VoteResult.ServerError("Fallo del servidor")

        // Set initial posts in UI state
        coEvery { feedRepository.getSavedPosts(testUserId) } returns flowOf(listOf(testPost))
        viewModel.setTab(CommunityTab.SAVED)

        assertEquals(0, viewModel.uiState.value.posts.first().myVoteValue)

        // Trigger toggleVote -> triggers rollback on ServerError
        viewModel.toggleVote(testPostId, 1)

        val revertedState = viewModel.uiState.value
        assertEquals(0, revertedState.posts.first().myVoteValue)
        assertEquals(
            com.farbalapps.rinde.util.UiText.DynamicString("Fallo del servidor"),
            revertedState.snackbarMessage
        )
    }

    @Test
    fun `toggleVote should debounce multiple rapid clicks within 400ms`() = runTest {
        coEvery { toggleVoteUseCase(testPostId, 1, any()) } returns VoteResult.Success(Triple(3, 1, 2))

        coEvery { feedRepository.getSavedPosts(testUserId) } returns flowOf(listOf(testPost))
        viewModel.setTab(CommunityTab.SAVED)

        // First click
        viewModel.toggleVote(testPostId, 1)
        // Second rapid click on same post immediately
        viewModel.toggleVote(testPostId, 1)

        // Only one call should be dispatched to toggleVoteUseCase
        coVerify(exactly = 1) { toggleVoteUseCase(testPostId, 1, any()) }
    }
}
