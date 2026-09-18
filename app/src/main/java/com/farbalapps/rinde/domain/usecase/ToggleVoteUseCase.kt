package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.community.UpdateAuthorTrustScoreUseCase
import javax.inject.Inject

sealed class VoteResult {
    data class Success(val counts: Triple<Int, Int, Int>?) : VoteResult()
    object Offline : VoteResult()
    data class NetworkError(val message: String) : VoteResult()
    data class ServerError(val message: String) : VoteResult()
}

/**
 * Caso de uso para registrar o alternar votos sobre ofertas de la comunidad.
 * 100% libre de dependencias del framework de Android, Room o WorkManager,
 * delegando la persistencia y sincronización en [FeedRepository].
 */
class ToggleVoteUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val updateAuthorTrustScoreUseCase: UpdateAuthorTrustScoreUseCase
) {
    suspend operator fun invoke(postId: String, voteValue: Int, authorId: String): VoteResult {
        val userId = authRepository.getCurrentUser()?.id
            ?: return VoteResult.ServerError("Usuario no autenticado")

        // 1. Optimistic Local Update
        feedRepository.applyOptimisticVote(postId, voteValue)

        if (!feedRepository.isNetworkAvailable()) {
            feedRepository.handleOfflineVote(userId, postId, voteValue, authorId)
            return VoteResult.Offline
        }

        return try {
            val result = feedRepository.toggleVoteTransaction(userId, postId, voteValue)
            if (result.isSuccess) {
                updateAuthorTrustScoreUseCase(authorId)
                val counts = feedRepository.fetchPostVoteCounts(postId).getOrNull()
                if (counts != null) {
                    feedRepository.syncLocalVoteAfterSuccess(postId, voteValue, counts)
                }
                VoteResult.Success(counts)
            } else {
                feedRepository.revertOptimisticVote(postId)
                classifyFailure(result.exceptionOrNull())
            }
        } catch (e: Exception) {
            feedRepository.revertOptimisticVote(postId)
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
}
