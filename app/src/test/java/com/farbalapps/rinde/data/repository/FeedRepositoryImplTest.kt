package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.repository.delegate.FeedInteractionDelegate
import com.farbalapps.rinde.data.repository.delegate.FeedLifecycleDelegate
import com.farbalapps.rinde.data.repository.delegate.FeedPaginationDelegate
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.VoteOverlay
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedRepositoryImplTest {

    private lateinit var paginationDelegate: FeedPaginationDelegate
    private lateinit var interactionDelegate: FeedInteractionDelegate
    private lateinit var lifecycleDelegate: FeedLifecycleDelegate
    private lateinit var repository: FeedRepositoryImpl

    private val postId = "post123"
    private val userId = "user123"

    @Before
    fun setUp() {
        paginationDelegate = mockk()
        interactionDelegate = mockk()
        lifecycleDelegate = mockk()

        // Mock StateFlow properties required by FeedRepository interface
        every { interactionDelegate.globalPostStatus } returns MutableStateFlow(emptyMap())
        every { interactionDelegate.globalSavedStatus } returns MutableStateFlow(emptyMap())
        every { interactionDelegate.globalVoteStatus } returns MutableStateFlow(emptyMap())

        repository = FeedRepositoryImpl(paginationDelegate, interactionDelegate, lifecycleDelegate)
    }

    @Test
    fun getPostById_delegatesToLifecycleDelegate() = runBlocking {
        val dummyPost = mockk<CommunityPost>()
        every { lifecycleDelegate.getPostById(postId) } returns flowOf(dummyPost)

        val result = repository.getPostById(postId).toList()

        assertEquals(1, result.size)
        assertEquals(dummyPost, result.first())
        verify(exactly = 1) { lifecycleDelegate.getPostById(postId) }
    }

    @Test
    fun toggleSave_delegatesToInteractionDelegate() = runBlocking {
        coEvery { interactionDelegate.toggleSave(userId, postId) } returns Result.success(Unit)

        val result = repository.toggleSave(userId, postId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { interactionDelegate.toggleSave(userId, postId) }
    }

    @Test
    fun toggleVote_delegatesToInteractionDelegate() = runBlocking {
        coEvery { interactionDelegate.toggleVote(userId, postId, 1) } returns Result.success(Unit)

        val result = repository.toggleVote(userId, postId, 1)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { interactionDelegate.toggleVote(userId, postId, 1) }
    }

    @Test
    fun deletePost_delegatesToLifecycleDelegate() = runBlocking {
        val photos = listOf("url1", "url2")
        coEvery { lifecycleDelegate.deletePost(postId, photos) } returns Result.success(Unit)

        val result = repository.deletePost(postId, photos)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { lifecycleDelegate.deletePost(postId, photos) }
    }
}
