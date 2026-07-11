package com.farbalapps.rinde.ui.screen.profile

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import com.farbalapps.rinde.domain.usecase.profile.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: ProfileViewModel
    
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getProfilePostsUseCase = mockk<GetProfilePostsUseCase>()
    private val getSavedPostsUseCase = mockk<GetSavedPostsUseCase>()
    private val updatePrivacyUseCase = mockk<UpdatePrivacyUseCase>()
    private val syncProfileUseCase = mockk<SyncProfileUseCase>()
    private val clearUploadStatusUseCase = mockk<ClearUploadStatusUseCase>()
    private val toggleVoteUseCase = mockk<ToggleVoteUseCase>()
    private val feedRepository = mockk<FeedRepository>()
    private val firebaseAuth = mockk<FirebaseAuth>()
    private val firebaseUser = mockk<FirebaseUser>()

    private val testUserId = "test_user_id"
    private val testProfile = Profile(
        id = testUserId,
        name = "Test User",
        email = "test@example.com",
        isDummy = false
    )
    private val testPosts = listOf(
        CommunityPost(
            id = "1",
            authorId = "author1",
            authorName = "Author One",
            authorPhotoUrl = null,
            timestamp = null,
            title = "Post 1",
            descriptionShort = "Short 1",
            descriptionLong = "Long 1",
            photos = emptyList(),
            category = "Tech",
            location = PostLocation("Store", null, null),
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
            offerType = OfferType.PHYSICAL,
            websiteName = null,
            productLink = null,
            storeName = null,
            isRecommended = false,
            expiresAt = null,
            normalPrice = 100.0,
            discountPrice = 80.0,
            currency = "MXN",
            couponCode = null,
            discountPercentage = 20,
            isAvailable = true,
            condition = "New"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns testUserId
        
        coEvery { getProfileUseCase(testUserId) } returns flowOf(testProfile)
        coEvery { getProfilePostsUseCase(testUserId) } returns flowOf(testPosts)
        coEvery { getSavedPostsUseCase(testUserId) } returns flowOf(emptyList())
        coEvery { syncProfileUseCase(testUserId) } returns Unit
        coEvery { clearUploadStatusUseCase(testUserId) } returns Result.success(Unit)
        coEvery { feedRepository.syncUserVotes(any()) } returns Result.success(Unit)
        coEvery { feedRepository.syncUserSavedPosts(any()) } returns Result.success(Unit)
        every { feedRepository.globalPostStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalSavedStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalVoteStatus } returns MutableStateFlow(emptyMap())
        
        viewModel = ProfileViewModel(
            getProfileUseCase,
            getProfilePostsUseCase,
            getSavedPostsUseCase,
            updatePrivacyUseCase,
            syncProfileUseCase,
            clearUploadStatusUseCase,
            toggleVoteUseCase,
            firebaseAuth,
            feedRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when viewmodel starts, it should load profile and posts`() = runTest {
        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            // Wait for all updates to settle
            testScheduler.advanceUntilIdle()
            
            val state = expectMostRecentItem()
            
            assertEquals(testProfile, state.profile)
            assertEquals(testPosts, state.posts)
            assertFalse("Loading should be false", state.isLoading)
            assertTrue("Should be current user", state.isCurrentUser)
        }
        
        coVerify { getProfileUseCase(testUserId) }
        coVerify { getProfilePostsUseCase(testUserId) }
        coVerify { syncProfileUseCase(testUserId) }
    }

    @Test
    fun `when profile is dummy, isLoading should be true`() = runTest {
        val dummyProfile = testProfile.copy(isDummy = true)
        coEvery { getProfileUseCase(testUserId) } returns flowOf(dummyProfile)
        
        viewModel = ProfileViewModel(
            getProfileUseCase,
            getProfilePostsUseCase,
            getSavedPostsUseCase,
            updatePrivacyUseCase,
            syncProfileUseCase,
            clearUploadStatusUseCase,
            toggleVoteUseCase,
            firebaseAuth,
            feedRepository
        )
        
        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.isLoading)
            assertEquals(dummyProfile, state.profile)
        }
    }
}
