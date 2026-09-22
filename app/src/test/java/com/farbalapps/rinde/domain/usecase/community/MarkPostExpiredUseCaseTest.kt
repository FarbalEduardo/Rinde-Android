package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.FeedRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MarkPostExpiredUseCaseTest {

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private lateinit var useCase: MarkPostExpiredUseCase

    @Before
    fun setup() {
        useCase = MarkPostExpiredUseCase(feedRepository)
    }

    @Test
    fun `markExpired should update local overlay and call repository`() = runTest {
        val postId = "post_123"
        coEvery { feedRepository.markPostAsExpired(postId) } returns Result.success(Unit)

        val result = useCase.markExpired(postId)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { feedRepository.updatePostStatusLocal(postId, VerificationStatus.EXPIRED) }
        coVerify(exactly = 1) { feedRepository.markPostAsExpired(postId) }
    }

    @Test
    fun `markAvailable should update local overlay and call repository`() = runTest {
        val postId = "post_123"
        coEvery { feedRepository.markPostAsAvailable(postId) } returns Result.success(Unit)

        val result = useCase.markAvailable(postId)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { feedRepository.updatePostStatusLocal(postId, VerificationStatus.PENDING) }
        coVerify(exactly = 1) { feedRepository.markPostAsAvailable(postId) }
    }
}
