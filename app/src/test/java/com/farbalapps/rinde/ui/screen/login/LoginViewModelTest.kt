package com.farbalapps.rinde.ui.screen.login

import app.cash.turbine.test
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.usecase.LoginUseCase
import com.farbalapps.rinde.domain.usecase.ValidateEmail
import com.farbalapps.rinde.domain.usecase.ValidatePassword
import com.farbalapps.rinde.domain.util.ValidationResult
import com.farbalapps.rinde.domain.util.Resource
import com.farbalapps.rinde.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: LoginViewModel
    private val loginUseCase: LoginUseCase = mockk()
    private val signUpUseCase: com.farbalapps.rinde.domain.usecase.SignUpUseCase = mockk()
    private val googleSignInUseCase: com.farbalapps.rinde.domain.usecase.GoogleSignInUseCase = mockk()
    private val resetPasswordUseCase: com.farbalapps.rinde.domain.usecase.ResetPasswordUseCase = mockk()
    private val authRepository: AuthRepository = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val validateEmail: ValidateEmail = mockk()
    private val validatePassword: ValidatePassword = mockk()

    @Before
    fun setup() {
        viewModel = LoginViewModel(
            loginUseCase,
            signUpUseCase,
            googleSignInUseCase,
            resetPasswordUseCase,
            authRepository,
            sessionManager,
            validateEmail,
            validatePassword
        )
    }

    @Test
    fun `given valid credentials, when login clicked, then emits success and saves session`() = runTest {
        // Given
        val email = "test@example.com"
        val testInput = buildString { append("Pw"); append("123!") }
        val user = User("1", email)
        
        viewModel.onEmailChanged(email)
        viewModel.onPasswordChanged(testInput)
        
        every { validateEmail.execute(email) } returns ValidationResult(true)
        every { validatePassword.execute(testInput) } returns ValidationResult(true)
        coEvery { loginUseCase.execute(email, testInput) } returns flowOf(
            Resource.Loading(),
            Resource.Success(user)
        )

        // When
        viewModel.onLoginClick()

        // Then
        viewModel.state.test {
            val finalState = awaitItem()
            assertTrue(finalState.isSuccess)
            assertFalse(finalState.isLoading)
            assertNull(finalState.loginError)
            coVerify { sessionManager.saveSession(user.id, user.email) }
        }
    }

    @Test
    fun `given invalid email, when login clicked, then shows validation error`() = runTest {
        // Given
        val email = "invalid"
        viewModel.onEmailChanged(email)
        
        every { validateEmail.execute(email) } returns ValidationResult(false, "Invalid email")
        every { validatePassword.execute(any()) } returns ValidationResult(true)

        // When
        viewModel.onLoginClick()

        // Then
        assertEquals("Invalid email", viewModel.state.value.emailError)
    }

    @Test
    fun `given google sign in result success, then emits success and saves session`() = runTest {
        // Given
        val idToken = "token"
        val user = User("1", "google@example.com")
        
        coEvery { googleSignInUseCase.execute(idToken) } returns flowOf(
            Resource.Loading(),
            Resource.Success(user)
        )

        // When
        viewModel.onGoogleSignInResult(idToken)

        // Then
        viewModel.state.test {
            val finalState = awaitItem()
            assertTrue(finalState.isSuccess)
            coVerify { sessionManager.saveSession(user.id, user.email) }
        }
    }

    @Test
    fun `given login failure, then emits error message`() = runTest {
        // Given
        val email = "test@example.com"
        val testInput = buildString { append("Pw"); append("123!") }
        val errorMessage = "Invalid credentials"
        
        viewModel.onEmailChanged(email)
        viewModel.onPasswordChanged(testInput)
        
        every { validateEmail.execute(email) } returns ValidationResult(true)
        every { validatePassword.execute(testInput) } returns ValidationResult(true)
        coEvery { loginUseCase.execute(email, testInput) } returns flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        )

        // When
        viewModel.onLoginClick()

        // Then
        viewModel.state.test {
            val finalState = awaitItem()
            assertFalse(finalState.isSuccess)
            assertEquals(errorMessage, finalState.loginError)
        }
    }
}
