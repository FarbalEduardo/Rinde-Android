package com.farbalapps.rinde.domain.usecase.account

import com.farbalapps.rinde.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SuspendAccountUseCaseTest {

    private lateinit var suspendAccountUseCase: SuspendAccountUseCase
    private val authRepository: AuthRepository = mockk()

    @Before
    fun setUp() {
        suspendAccountUseCase = SuspendAccountUseCase(authRepository)
    }

    @Test
    fun `when userId is blank, returns failure without calling repository`() = runTest {
        val result = suspendAccountUseCase("")
        assertTrue(result.isFailure)
        assertEquals("El ID del usuario no puede estar vacío", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { authRepository.suspendAccount(any()) }
    }

    @Test
    fun `when userId is valid and repository succeeds, returns success`() = runTest {
        coEvery { authRepository.suspendAccount("user_123") } returns Result.success(Unit)

        val result = suspendAccountUseCase("user_123")
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.suspendAccount("user_123") }
    }

    @Test
    fun `when repository fails, propagates failure`() = runTest {
        coEvery { authRepository.suspendAccount("user_123") } returns Result.failure(RuntimeException("Network error"))

        val result = suspendAccountUseCase("user_123")
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
