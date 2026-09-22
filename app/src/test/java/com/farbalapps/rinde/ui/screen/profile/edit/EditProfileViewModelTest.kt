package com.farbalapps.rinde.ui.screen.profile.edit

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.usecase.account.ChangePasswordUseCase
import com.farbalapps.rinde.domain.usecase.account.DeleteAccountUseCase
import com.farbalapps.rinde.domain.usecase.account.SuspendAccountUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.UpdatePrivacyUseCase
import com.farbalapps.rinde.domain.usecase.profile.UpdateProfileUseCase
import com.farbalapps.rinde.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getProfileUseCase: GetProfileUseCase = mockk()
    private val updateProfileUseCase: UpdateProfileUseCase = mockk()
    private val updatePrivacyUseCase: UpdatePrivacyUseCase = mockk()
    private val changePasswordUseCase: ChangePasswordUseCase = mockk()
    private val suspendAccountUseCase: SuspendAccountUseCase = mockk()
    private val deleteAccountUseCase: DeleteAccountUseCase = mockk()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val firebaseAuth: FirebaseAuth = mockk()
    private val firebaseUser: FirebaseUser = mockk()

    private lateinit var viewModel: EditProfileViewModel

    @Before
    fun setUp() {
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "user_123"
        every { firebaseUser.email } returns "user@example.com"
        every { firebaseUser.providerData } returns emptyList()

        val sampleProfile = Profile(
            id = "user_123",
            name = "Test User",
            email = "user@example.com",
            photoUrl = "https://example.com/photo.jpg",
            isPrivate = false
        )
        every { getProfileUseCase("user_123") } returns flowOf(sampleProfile)

        viewModel = EditProfileViewModel(
            getProfileUseCase,
            updateProfileUseCase,
            updatePrivacyUseCase,
            changePasswordUseCase,
            suspendAccountUseCase,
            deleteAccountUseCase,
            authRepository,
            firebaseAuth
        )
    }

    @Test
    fun `initial state loads profile correctly`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Test User", state.name)
            assertEquals("https://example.com/photo.jpg", state.photoUrl)
            assertFalse(state.isPrivate)
            assertEquals("user@example.com", state.email)
            assertEquals(EditProfileActiveDialog.NONE, state.activeDialog)
        }
    }

    @Test
    fun `open and dismiss change password dialog updates state`() = runTest {
        viewModel.openChangePasswordDialog()
        assertEquals(EditProfileActiveDialog.CHANGE_PASSWORD, viewModel.uiState.value.activeDialog)

        viewModel.dismissDialog()
        assertEquals(EditProfileActiveDialog.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun `open account management dialog flow updates state correctly`() = runTest {
        viewModel.openAccountManagementDialog()
        assertEquals(EditProfileActiveDialog.SOFT_OPTIONS, viewModel.uiState.value.activeDialog)

        viewModel.proceedToPermanentDeleteDialog()
        assertEquals(EditProfileActiveDialog.PERMANENT_DELETE, viewModel.uiState.value.activeDialog)

        viewModel.dismissDialog()
        assertEquals(EditProfileActiveDialog.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun `when changePassword succeeds, sets changePasswordSuccess and closes dialog`() = runTest {
        coEvery { changePasswordUseCase("oldPass", "newPass", "newPass") } returns Result.success(Unit)

        viewModel.openChangePasswordDialog()
        viewModel.changePassword("oldPass", "newPass", "newPass")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.changePasswordSuccess)
            assertEquals(EditProfileActiveDialog.NONE, state.activeDialog)
            assertNull(state.changePasswordError)
        }
    }

    @Test
    fun `when changePassword fails, sets error message and dialog remains open`() = runTest {
        coEvery { changePasswordUseCase("wrongOld", "newPass", "newPass") } returns Result.failure(Exception("Contraseña actual incorrecta"))

        viewModel.openChangePasswordDialog()
        viewModel.changePassword("wrongOld", "newPass", "newPass")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.changePasswordSuccess)
            assertEquals("Contraseña actual incorrecta", state.changePasswordError)
        }
    }

    @Test
    fun `when suspendAccount succeeds, sets isAccountSuspended true`() = runTest {
        coEvery { suspendAccountUseCase("user_123") } returns Result.success(Unit)

        viewModel.suspendAccount()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAccountSuspended)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `when deleteAccountPermanently succeeds, sets isAccountDeleted true`() = runTest {
        coEvery { deleteAccountUseCase("user_123") } returns Result.success(Unit)

        viewModel.deleteAccountPermanently()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAccountDeleted)
            assertFalse(state.isLoading)
        }
    }
}
