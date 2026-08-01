package com.farbalapps.rinde.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.farbalapps.rinde.data.worker.VoteSyncWorker
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.community.UpdateAuthorTrustScoreUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.farbalapps.rinde.data.local.dao.PostDao

sealed class VoteResult {
    data class Success(val counts: Triple<Int, Int, Int>?) : VoteResult()
    object Offline : VoteResult()
    data class NetworkError(val message: String) : VoteResult()
    data class ServerError(val message: String) : VoteResult()
}

class ToggleVoteUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val updateAuthorTrustScoreUseCase: UpdateAuthorTrustScoreUseCase,
    private val workManager: WorkManager,
    private val postDao: PostDao,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(postId: String, voteValue: Int, authorId: String): VoteResult {
        val userId = authRepository.getCurrentUser()?.id
            ?: return VoteResult.ServerError("Usuario no autenticado")

        // 1. Optimistic Local Update
        val existingPost = postDao.getPostById(postId)
        val originalState = extractOriginalState(existingPost)

        applyOptimisticUpdate(postId, voteValue, existingPost, originalState)

        if (!isNetworkAvailable()) {
            feedRepository.savePendingVote(userId, postId, voteValue, authorId)
            enqueueSyncWorker()
            return VoteResult.Offline
        }

        return try {
            val result = feedRepository.toggleVoteTransaction(userId, postId, voteValue)
            if (result.isSuccess) {
                handleSuccess(authorId, postId, voteValue, originalState.vote)
                VoteResult.Success(feedRepository.fetchPostVoteCounts(postId).getOrNull())
            } else {
                revertOptimisticUpdate(postId, existingPost, originalState)
                classifyFailure(result.exceptionOrNull())
            }
        } catch (e: Exception) {
            revertOptimisticUpdate(postId, existingPost, originalState)
            classifyFailure(e)
        }
    }

    private data class OriginalState(val vote: Int, val truthCount: Int, val falseCount: Int, val score: Int)

    private fun extractOriginalState(existingPost: com.farbalapps.rinde.data.local.entity.CommunityPostEntity?): OriginalState {
        return OriginalState(
            vote = existingPost?.myVoteValue ?: 0,
            truthCount = existingPost?.truthCount ?: 0,
            falseCount = existingPost?.falseCount ?: 0,
            score = existingPost?.votesScore ?: 0
        )
    }

    private suspend fun applyOptimisticUpdate(
        postId: String, 
        voteValue: Int, 
        existingPost: com.farbalapps.rinde.data.local.entity.CommunityPostEntity?,
        originalState: OriginalState
    ) {
        if (existingPost != null) {
            val nextVote = if (originalState.vote == voteValue) 0 else voteValue
            val tDelta = (if (nextVote == 1) 1 else 0) - (if (originalState.vote == 1) 1 else 0)
            val fDelta = (if (nextVote == -1) 1 else 0) - (if (originalState.vote == -1) 1 else 0)

            val optTruth = (originalState.truthCount + tDelta).coerceAtLeast(0)
            val optFalse = (originalState.falseCount + fDelta).coerceAtLeast(0)

            postDao.updateVoteState(
                postId, nextVote,
                optTruth,
                optFalse,
                originalState.score + (nextVote - originalState.vote)
            )
            feedRepository.updateVoteStatusLocal(
                postId,
                com.farbalapps.rinde.domain.repository.VoteOverlay(
                    truthCount = optTruth,
                    falseCount = optFalse,
                    myVote = nextVote
                )
            )
        }
    }

    private suspend fun revertOptimisticUpdate(
        postId: String, 
        existingPost: com.farbalapps.rinde.data.local.entity.CommunityPostEntity?,
        originalState: OriginalState
    ) {
        if (existingPost != null) {
            postDao.updateVoteState(postId, originalState.vote, originalState.truthCount, originalState.falseCount, originalState.score)
            feedRepository.updateVoteStatusLocal(
                postId,
                com.farbalapps.rinde.domain.repository.VoteOverlay(
                    truthCount = originalState.truthCount,
                    falseCount = originalState.falseCount,
                    myVote = originalState.vote
                )
            )
        }
    }

    private suspend fun handleSuccess(authorId: String, postId: String, voteValue: Int, originalVote: Int) {
        updateAuthorTrustScoreUseCase(authorId)
        val counts = feedRepository.fetchPostVoteCounts(postId)
        counts.getOrNull()?.let { (truth, false_, score) ->
            val local = postDao.getPostById(postId)
            val currentVote = local?.myVoteValue ?: (if (originalVote == voteValue) 0 else voteValue)
            if (local != null) {
                postDao.updateVoteState(postId, currentVote, truth, false_, score)
            }
            feedRepository.updateVoteStatusLocal(
                postId,
                com.farbalapps.rinde.domain.repository.VoteOverlay(
                    truthCount = truth,
                    falseCount = false_,
                    myVote = currentVote
                )
            )
        }
    }

    private fun classifyFailure(e: Throwable?): VoteResult {
        return when {
            e is java.net.UnknownHostException ||
                    e is java.net.SocketTimeoutException ->
                VoteResult.NetworkError("Sin conexión a internet. Tu voto se guardó y se enviará al reconectar.")
            e?.message?.contains("PERMISSION_DENIED") == true ->
                VoteResult.ServerError("No tienes permiso para votar en esta publicación.")
            e?.message?.contains("UNAVAILABLE") == true ->
                VoteResult.NetworkError("Servidor no disponible. Reintentando automáticamente.")
            e?.message?.contains("ABORTED") == true ->
                VoteResult.NetworkError("Hubo un conflicto. Reintentando...")
            else ->
                VoteResult.ServerError("No se pudo registrar el voto: ${e?.message ?: "error desconocido"}")
        }
    }

    private fun enqueueSyncWorker() {
        val request = OneTimeWorkRequestBuilder<VoteSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "vote_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
