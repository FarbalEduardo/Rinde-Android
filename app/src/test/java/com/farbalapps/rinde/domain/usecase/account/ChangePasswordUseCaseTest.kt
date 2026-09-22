package com.farbalapps.rinde.domain.usecase.account

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.usecase.ValidatePassword
import com.farbalapps.rinde.domain.util.ValidationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChangePasswordUseCaseTest {

    private lateinit var changePasswordUseCase: ChangePasswordUseCase
    private val authRepository: AuthRepository = mockk()
    private val validatePassword: ValidatePassword = mockk()

    @Before
    fun setUp() {
        changePasswordUseCase = ChangePasswordUseCase(authRepository, validatePassword)
    }

    @Test
    fun `when fields are blank, returns failure without calling repository`() = runTest {
        val result = changePasswordUseCase("", "NewPass123", "NewPass123")
        assertTrue(result.isFailure)
        assertEquals("Todos los campos son obligatorios", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
    }

    @Test
    fun `when new passwords do not match, returns failure`() = runTest {
        val result = changePasswordUseCase("CurrentPass123", "NewPass123", "MismatchPass123")
        assertTrue(result.isFailure)
        assertEquals("Las nuevas contraseñas no coinciden", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
    }

    @Test
    fun `when new password is the same as current password, returns failure`() = runTest {
        val result = changePasswordUseCase("SamePass123", "SamePass123", "SamePass123")
        assertTrue(result.isFailure)
        assertEquals("La nueva contraseña debe ser diferente a la actual", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
    }

    @Test
    fun `when new password does not pass format validation, returns failure`() = runTest {
        every { validatePassword.execute("weak") } returns ValidationResult(false, "Mínimo 6 caracteres")

        val result = changePasswordUseCase("CurrentPass123", "weak", "weak")
        assertTrue(result.isFailure)
        assertEquals("Mínimo 6 caracteres", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
    }

    @Test
    fun `when all validations pass, calls repository and returns success`() = runTest {
        every { validatePassword.execute("ValidNewPass123") } returns ValidationResult(true)
        coEvery { authRepository.changePassword("CurrentPass123", "ValidNewPass123") } returns Result.success(Unit)

        val result = changePasswordUseCase("CurrentPass123", "ValidNewPass123", "ValidNewPass123")
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.changePassword("CurrentPass123", "ValidNewPass123") }
    }
}
