package com.farbalapps.rinde.ui.screen.profile

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.ProfilePost
import com.farbalapps.rinde.domain.usecase.profile.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val followUserUseCase = mockk<FollowUserUseCase>()
    private val unfollowUserUseCase = mockk<UnfollowUserUseCase>()
    private val isFollowingUseCase = mockk<IsFollowingUseCase>()
    private val updatePrivacyUseCase = mockk<UpdatePrivacyUseCase>()
    private val syncProfileUseCase = mockk<SyncProfileUseCase>()
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
        ProfilePost(id = "1", title = "Post 1", description = "Desc 1", timeLocation = "Today", imageUrl = null)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns testUserId
        
        coEvery { getProfileUseCase(testUserId) } returns flowOf(testProfile)
        coEvery { getProfilePostsUseCase(testUserId) } returns flowOf(testPosts)
        coEvery { syncProfileUseCase(testUserId) } returns Unit
        
        viewModel = ProfileViewModel(
            getProfileUseCase,
            getProfilePostsUseCase,
            followUserUseCase,
            unfollowUserUseCase,
            isFollowingUseCase,
            updatePrivacyUseCase,
            syncProfileUseCase,
            firebaseAuth
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when viewmodel starts, it should load profile and posts`() = runTest {
        viewModel.uiState.test {
            // Wait for all updates to settle
            testScheduler.advanceUntilIdle()
            
            val state = expectMostRecentItem()
            
            assertEquals(testProfile, state.profile)
            assertEquals(testPosts, state.posts)
            assertFalse("Loading should be false", state.isLoading)
            assertTrue("Should be current user", state.isCurrentUser)
            assertTrue("Debug info should be present", state.debugInfo.isNotEmpty())
        }
        
        coVerify { getProfileUseCase(testUserId) }
        coVerify { getProfilePostsUseCase(testUserId) }
        coVerify { syncProfileUseCase(testUserId) }
    }

    @Test
    fun `when profile is dummy, isLoading should be true`() = runTest {
        val dummyProfile = testProfile.copy(isDummy = true)
        coEvery { getProfileUseCase(testUserId) } returns flowOf(dummyProfile)
        
        // Re-init viewmodel to use the new mock
        viewModel = ProfileViewModel(
            getProfileUseCase,
            getProfilePostsUseCase,
            followUserUseCase,
            unfollowUserUseCase,
            isFollowingUseCase,
            updatePrivacyUseCase,
            syncProfileUseCase,
            firebaseAuth
        )
        
        viewModel.uiState.test {
            testScheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.isLoading)
            assertEquals(dummyProfile, state.profile)
        }
    }
}
