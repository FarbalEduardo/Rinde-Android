package com.farbalapps.rinde.ui.screen.signup

import app.cash.turbine.test
import com.farbalapps.rinde.domain.usecase.SignUpUseCase
import com.farbalapps.rinde.domain.util.Resource
import com.farbalapps.rinde.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SignUpViewModel
    private val signUpUseCase: SignUpUseCase = mockk()
    private val googleSignInUseCase: com.farbalapps.rinde.domain.usecase.GoogleSignInUseCase = mockk()
    private val sessionManager: com.farbalapps.rinde.data.local.SessionManager = mockk(relaxed = true)

    @Before
    fun setup() {
        viewModel = SignUpViewModel(
            signUpUseCase,
            googleSignInUseCase,
            sessionManager
        )
    }

    @Test
    fun `given valid data, when sign up called, then emits success`() = runTest {
        // Given
        val fullName = "John Doe"
        val email = "john@example.com"
        val testInput = buildString { append("Pw"); append("123!") }
        
        coEvery { signUpUseCase.execute(fullName, email, testInput) } returns flowOf(
            Resource.Loading(),
            Resource.Success(true)
        )

        // When
        viewModel.signUp(fullName, email, testInput)

        // Then
        viewModel.state.test {
            val finalState = awaitItem()
            assertTrue(finalState.isSuccess)
            assertFalse(finalState.isLoading)
            assertNull(finalState.error)
        }
    }

    @Test
    fun `given signup failure, then emits error message`() = runTest {
        // Given
        val fullName = "John Doe"
        val email = "john@example.com"
        val testInput = buildString { append("Pw"); append("123!") }
        val errorMessage = "Email already in use"
        
        coEvery { signUpUseCase.execute(fullName, email, testInput) } returns flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        )

        // When
        viewModel.signUp(fullName, email, testInput)

        // Then
        viewModel.state.test {
            val finalState = awaitItem()
            assertFalse(finalState.isSuccess)
            assertEquals(errorMessage, finalState.error)
        }
    }

    @Test
    fun `when clearError called, then error is null`() = runTest {
        // Given
        val fullName = "John Doe"
        val email = "john@example.com"
        val testInput = buildString { append("Pw"); append("123!") }
        coEvery { signUpUseCase.execute(any(), any(), any()) } returns flowOf(Resource.Error("Error"))
        viewModel.signUp(fullName, email, testInput)
        
        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.state.value.error)
    }
}
