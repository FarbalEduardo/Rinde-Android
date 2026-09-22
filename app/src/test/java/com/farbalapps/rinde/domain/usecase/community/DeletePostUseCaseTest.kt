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

class DeletePostUseCaseTest {

    private val feedRepository = mockk<FeedRepository>()
    private lateinit var useCase: DeletePostUseCase

    @Before
    fun setup() {
        useCase = DeletePostUseCase(feedRepository)
    }

    @Test
    fun `invoke should call repository deletePost and return success`() = runTest {
        val postId = "post_123"
        val photos = listOf("https://res.cloudinary.com/photo1.jpg")
        coEvery { feedRepository.deletePost(postId, photos) } returns Result.success(Unit)

        val result = useCase(postId, photos)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { feedRepository.deletePost(postId, photos) }
    }

    @Test
    fun `invoke should return failure when postId is blank`() = runTest {
        val result = useCase("", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { feedRepository.deletePost(any(), any()) }
    }
}
