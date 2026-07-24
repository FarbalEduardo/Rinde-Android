package com.farbalapps.rinde.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.farbalapps.rinde.data.local.dao.PendingVoteDao
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.UserVoteDao
import com.farbalapps.rinde.data.local.entity.PendingVoteEntity
import com.farbalapps.rinde.data.local.entity.UserVoteEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class VoteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingVoteDao: PendingVoteDao,
    private val userVoteDao: UserVoteDao,
    private val postDao: PostDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    companion object {
        const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val pending = pendingVoteDao.getAll()
        if (pending.isEmpty()) return Result.success()

        var hasFailures = false

        for (vote in pending) {
            if (vote.retryCount >= MAX_RETRIES) {
                // Demasiados intentos fallidos: limpiar y revertir
                pendingVoteDao.delete(vote.postId, vote.userId)
                postDao.updateVoteValueOnly(vote.postId, 0)
                continue
            }

            try {
                // Ejecutar la transacción de voto
                val actualNextVote = sendVoteTransaction(vote)

                // Actualizar tabla de votos de usuario en Room
                if (actualNextVote == 0) {
                    userVoteDao.deleteVote(vote.postId, vote.userId)
                } else {
                    userVoteDao.upsertVote(UserVoteEntity(vote.postId, vote.userId, actualNextVote))
                }

                // Sincronizar contadores frescos en Room
                refreshPostCounts(vote.postId)

                // Remover de la lista de pendientes
                pendingVoteDao.delete(vote.postId, vote.userId)

            } catch (e: Exception) {
                hasFailures = true
                val reason = classifyError(e)
                pendingVoteDao.upsert(
                    vote.copy(
                        retryCount = vote.retryCount + 1,
                        lastFailureReason = reason
                    )
                )
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    private suspend fun sendVoteTransaction(vote: PendingVoteEntity): Int {
        val voteDocRef = firestore.collection("posts")
            .document(vote.postId).collection("votes").document(vote.userId)
        val postRef = firestore.collection("posts").document(vote.postId)

        var appliedVote = 0
        firestore.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteDocRef)
            val serverVote = voteSnapshot.getLong("value")?.toInt() ?: 0

            val nextVote = if (serverVote == vote.voteValue) 0 else vote.voteValue

            val truthDelta = (if (nextVote == 1) 1 else 0) - (if (serverVote == 1) 1 else 0)
            val falseDelta = (if (nextVote == -1) 1 else 0) - (if (serverVote == -1) 1 else 0)
            val scoreDelta = nextVote - serverVote

            if (nextVote == 0) {
                transaction.delete(voteDocRef)
            } else {
                transaction.set(voteDocRef, mapOf(
                    "value" to nextVote,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }

            if (truthDelta != 0 || falseDelta != 0) {
                transaction.update(
                    postRef,
                    "truthCount", FieldValue.increment(truthDelta.toLong()),
                    "falseCount", FieldValue.increment(falseDelta.toLong()),
                    "votesScore", FieldValue.increment(scoreDelta.toLong())
                )
            }
            appliedVote = nextVote
        }.await()

        return appliedVote
    }

    private suspend fun refreshPostCounts(postId: String) {
        val doc = firestore.collection("posts").document(postId).get().await()
        val entity = postDao.getPostById(postId) ?: return
        postDao.upsertPosts(
            listOf(
                entity.copy(
                    truthCount = doc.getLong("truthCount")?.toInt() ?: entity.truthCount,
                    falseCount = doc.getLong("falseCount")?.toInt() ?: entity.falseCount,
                    votesScore = doc.getLong("votesScore")?.toInt() ?: entity.votesScore
                )
            )
        )
    }

    private fun classifyError(e: Exception): String = when {
        e is java.net.UnknownHostException ||
                e is java.net.SocketTimeoutException -> "Sin conexión a internet"
        e.message?.contains("PERMISSION_DENIED") == true -> "No tienes permiso para votar"
        e.message?.contains("UNAVAILABLE") == true -> "Servidor no disponible, reintentando"
        e.message?.contains("ABORTED") == true -> "Conflicto de datos, reintentando"
        else -> "Error al enviar voto (${e.javaClass.simpleName})"
    }
}
