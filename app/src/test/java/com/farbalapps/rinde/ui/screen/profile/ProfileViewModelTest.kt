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
            timestamp = 0L,
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
            testScheduler.runCurrent()
            
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
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertTrue(state.isLoading)
            assertEquals(dummyProfile, state.profile)
        }
    }

    @Test
    fun `when posts have less than 2 votes, computedRating should be null`() = runTest {
        val validatingPost = testPosts[0].copy(truthCount = 1, falseCount = 0)
        coEvery { getProfilePostsUseCase(testUserId) } returns flowOf(listOf(validatingPost))

        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertEquals(null, state.computedRating)
            assertEquals(0, state.ratedPostsCount)
        }
    }

    @Test
    fun `when posts are 100 percent real, computedRating should be 5`() = runTest {
        val hundredPercentRealPost = testPosts[0].copy(truthCount = 10, falseCount = 0)
        coEvery { getProfilePostsUseCase(testUserId) } returns flowOf(listOf(hundredPercentRealPost))

        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertEquals(5.0f, state.computedRating!!, 0.01f)
            assertEquals(1, state.ratedPostsCount)
        }
    }

    @Test
    fun `when posts are 100 percent false, computedRating should be 1`() = runTest {
        val hundredPercentFalsePost = testPosts[0].copy(truthCount = 0, falseCount = 10)
        coEvery { getProfilePostsUseCase(testUserId) } returns flowOf(listOf(hundredPercentFalsePost))

        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertEquals(1.0f, state.computedRating!!, 0.01f)
            assertEquals(1, state.ratedPostsCount)
        }
    }

    @Test
    fun `when posts are 50 percent real, computedRating should be 3`() = runTest {
        val fiftyPercentPost = testPosts[0].copy(truthCount = 5, falseCount = 5)
        coEvery { getProfilePostsUseCase(testUserId) } returns flowOf(listOf(fiftyPercentPost))

        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertEquals(3.0f, state.computedRating!!, 0.01f)
            assertEquals(1, state.ratedPostsCount)
        }
    }

    @Test
    fun `when viewing another user profile, isCurrentUser should be false`() = runTest {
        val otherUserId = "other_user_123"
        val otherProfile = testProfile.copy(id = otherUserId)
        coEvery { getProfileUseCase(otherUserId) } returns flowOf(otherProfile)
        coEvery { getProfilePostsUseCase(otherUserId) } returns flowOf(emptyList())

        viewModel.loadProfile(otherUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertFalse(state.isCurrentUser)
            assertEquals(otherProfile, state.profile)
        }
    }

    @Test
    fun `when profile has custom photoUrl, uiState holds photoUrl correctly`() = runTest {
        val customPhotoUrl = "https://res.cloudinary.com/demo/image/upload/v1/user.jpg"
        val profileWithPhoto = testProfile.copy(photoUrl = customPhotoUrl)
        coEvery { getProfileUseCase(testUserId) } returns flowOf(profileWithPhoto)

        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertEquals(customPhotoUrl, state.profile?.photoUrl)
        }
    }

    @Test
    fun `when profile photoUrl is null or empty, uiState holds null photoUrl`() = runTest {
        val profileWithoutPhoto = testProfile.copy(photoUrl = null)
        coEvery { getProfileUseCase(testUserId) } returns flowOf(profileWithoutPhoto)

        viewModel.loadProfile(testUserId)
        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            assertEquals(null, state.profile?.photoUrl)
        }
    }
}
