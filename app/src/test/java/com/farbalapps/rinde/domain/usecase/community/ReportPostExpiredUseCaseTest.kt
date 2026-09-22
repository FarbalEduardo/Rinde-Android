package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.repository.FeedRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportPostExpiredUseCaseTest {

    private val feedRepository = mockk<FeedRepository>()
    private lateinit var useCase: ReportPostExpiredUseCase

    @Before
    fun setup() {
        useCase = ReportPostExpiredUseCase(feedRepository)
    }

    @Test
    fun `invoke should delegate to repository with correct parameters`() = runTest {
        val postId = "post_1"
        val postTitle = "Oferta Leche"
        val authorId = "author_1"
        val currentUserId = "user_2"
        val currentUserName = "Reporter"

        coEvery {
            feedRepository.reportPostAsExpired(postId, postTitle, authorId, currentUserId, currentUserName)
        } returns Result.success(Unit)

        val result = useCase(postId, postTitle, authorId, currentUserId, currentUserName)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            feedRepository.reportPostAsExpired(postId, postTitle, authorId, currentUserId, currentUserName)
        }
    }
}
