package com.farbalapps.rinde.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.WorkManager
import com.farbalapps.rinde.domain.model.User
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.community.UpdateAuthorTrustScoreUseCase
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.farbalapps.rinde.data.local.dao.PostDao

class ToggleVoteUseCaseTest {

    private lateinit var feedRepository: FeedRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var updateAuthorTrustScoreUseCase: UpdateAuthorTrustScoreUseCase
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val connectivityManager = mockk<ConnectivityManager>()
    private val networkCapabilities = mockk<NetworkCapabilities>()
    private val postDao = mockk<PostDao>(relaxed = true)
    private lateinit var toggleVoteUseCase: ToggleVoteUseCase

    @Before
    fun setUp() {
        feedRepository = mockk(relaxed = true)
        authRepository = mockk()
        updateAuthorTrustScoreUseCase = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns mockk()
        every { connectivityManager.getNetworkCapabilities(any()) } returns networkCapabilities
        // Red disponible por defecto
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        toggleVoteUseCase = ToggleVoteUseCase(
            feedRepository,
            authRepository,
            updateAuthorTrustScoreUseCase,
            workManager,
            postDao,
            context
        )
    }

    @Test
    fun `cuando el usuario no esta autenticado debe fallar`() = runBlocking {
        every { authRepository.getCurrentUser() } returns null

        val result = toggleVoteUseCase("post123", 1, "author456")

        assertTrue(result is VoteResult.ServerError)
        assertEquals("Usuario no autenticado", (result as VoteResult.ServerError).message)
    }

    @Test
    fun `cuando el repo falla debe devolver error y no actualizar reputacion`() = runBlocking {
        val userId = "user123"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.toggleVoteTransaction(userId, "post123", 1) } returns Result.failure(Exception("DB Error"))

        val result = toggleVoteUseCase("post123", 1, "author456")

        assertTrue(result is VoteResult.ServerError)
        coVerify(exactly = 0) { updateAuthorTrustScoreUseCase(any()) }
    }

    @Test
    fun `cuando el voto es exitoso debe actualizar la reputacion del autor`() = runBlocking {
        val userId = "user123"
        val authorId = "author456"
        every { authRepository.getCurrentUser() } returns User(id = userId, email = "test@test.com")
        coEvery { feedRepository.toggleVoteTransaction(userId, "post123", 1) } returns Result.success(Triple(5, 1, 4))
        coEvery { feedRepository.fetchPostVoteCounts("post123") } returns Result.success(Triple(5, 1, 4))

        val result = toggleVoteUseCase("post123", 1, authorId)

        assertTrue(result is VoteResult.Success)
        coVerify(exactly = 1) { feedRepository.toggleVoteTransaction(userId, "post123", 1) }
        coVerify(exactly = 1) { updateAuthorTrustScoreUseCase(authorId) }
    }
}
