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
        var originalVote = 0
        var originalTruthCount = 0
        var originalFalseCount = 0
        var originalScore = 0

        if (existingPost != null) {
            originalVote = existingPost.myVoteValue
            originalTruthCount = existingPost.truthCount
            originalFalseCount = existingPost.falseCount
            originalScore = existingPost.votesScore

            val nextVote = if (originalVote == voteValue) 0 else voteValue
            val tDelta = (if (nextVote == 1) 1 else 0) - (if (originalVote == 1) 1 else 0)
            val fDelta = (if (nextVote == -1) 1 else 0) - (if (originalVote == -1) 1 else 0)

            postDao.updateVoteState(
                postId, nextVote,
                (originalTruthCount + tDelta).coerceAtLeast(0),
                (originalFalseCount + fDelta).coerceAtLeast(0),
                originalScore + (nextVote - originalVote)
            )
        }

        if (!isNetworkAvailable()) {
            feedRepository.savePendingVote(userId, postId, voteValue, authorId)
            enqueueSyncWorker()
            return VoteResult.Offline
        }

        return try {
            val result = feedRepository.toggleVoteTransaction(userId, postId, voteValue)
            if (result.isSuccess) {
                updateAuthorTrustScoreUseCase(authorId)
                val counts = feedRepository.fetchPostVoteCounts(postId)
                counts.getOrNull()?.let { (truth, false_, score) ->
                    val local = postDao.getPostById(postId)
                    if (local != null) {
                        postDao.updateVoteState(postId, local.myVoteValue, truth, false_, score)
                    }
                }
                VoteResult.Success(counts.getOrNull())
            } else {
                // Revert optimistic update
                if (existingPost != null) {
                    postDao.updateVoteState(postId, originalVote, originalTruthCount, originalFalseCount, originalScore)
                }
                val e = result.exceptionOrNull()
                classifyFailure(e)
            }
        } catch (e: Exception) {
            // Revert optimistic update
            if (existingPost != null) {
                postDao.updateVoteState(postId, originalVote, originalTruthCount, originalFalseCount, originalScore)
            }
            classifyFailure(e)
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
