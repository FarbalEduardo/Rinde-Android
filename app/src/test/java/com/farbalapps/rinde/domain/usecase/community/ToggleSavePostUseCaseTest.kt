package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.repository.FeedRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleSavePostUseCaseTest {

    private val feedRepository = mockk<FeedRepository>()
    private lateinit var useCase: ToggleSavePostUseCase

    @Before
    fun setup() {
        useCase = ToggleSavePostUseCase(feedRepository)
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        val userId = "user_123"
        val postId = "post_456"
        coEvery { feedRepository.toggleSave(userId, postId) } returns Result.success(Unit)

        val result = useCase(userId, postId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { feedRepository.toggleSave(userId, postId) }
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        val userId = "user_123"
        val postId = "post_456"
        val error = RuntimeException("Network error")
        coEvery { feedRepository.toggleSave(userId, postId) } returns Result.failure(error)

        val result = useCase(userId, postId)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        coVerify(exactly = 1) { feedRepository.toggleSave(userId, postId) }
    }

    @Test
    fun `invoke should return failure when parameters are blank`() = runTest {
        val result = useCase("", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { feedRepository.toggleSave(any(), any()) }
    }
}
