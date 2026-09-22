package com.farbalapps.rinde.ui.screen.home.community

import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.usecase.CreatePostUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.farbalapps.rinde.util.LocationService
import com.farbalapps.rinde.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createPostUseCase = mockk<CreatePostUseCase>(relaxed = true)
    private val getProfileUseCase = mockk<GetProfileUseCase>(relaxed = true)
    private val locationService = mockk<LocationService>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    private lateinit var viewModel: CreatePostViewModel

    @Before
    fun setup() {
        every { authRepository.getCurrentUser() } returns mockk {
            every { id } returns "user_test_123"
        }
        every { getProfileUseCase("user_test_123") } returns flowOf(
            Profile(id = "user_test_123", isPrivate = false)
        )

        viewModel = CreatePostViewModel(
            createPostUseCase = createPostUseCase,
            getProfileUseCase = getProfileUseCase,
            locationService = locationService,
            authRepository = authRepository
        )
    }

    @Test
    fun `initial state should observe user profile privacy without firebase sdk`() = runTest {
        assertFalse(viewModel.uiState.value.isPrivateProfile)
        assertEquals(OfferType.ONLINE, viewModel.uiState.value.offerType)
    }

    @Test
    fun `onOfferTypeChange should update offerType in uiState`() = runTest {
        viewModel.onOfferTypeChange(OfferType.PHYSICAL)
        assertEquals(OfferType.PHYSICAL, viewModel.uiState.value.offerType)
    }
}

