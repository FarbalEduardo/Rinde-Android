package com.farbalapps.rinde.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.util.NotificationHelper
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.util.Date

@HiltWorker
class FeedSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannels(context)

        return try {
            val meta = syncMetadataDao.getMetadata("feed_global")
            val hasCachedData = postDao.getPostsOnce(1).isNotEmpty()

            if (!hasCachedData) {
                // Si no hay datos cacheados, el ViewModel o primera carga se encargará.
                return Result.success()
            }

            val sinceTimestamp = meta?.lastSyncTimestamp ?: 0L
            val now = System.currentTimeMillis()
            val elapsedMinutes = (now - sinceTimestamp) / (60 * 1000)

            // Validar TTL de 10 minutos para no consumir demasiadas peticiones
            if (elapsedMinutes < 10) {
                return Result.success()
            }

            // Consulta tipo COUNT-only en Firestore para saber si hay posts nuevos (más barata en costo)
            val countQuery = firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .whereGreaterThan("timestamp", Date(sinceTimestamp))
            
            val newCount = countQuery.count().get(AggregateSource.SERVER).await().count.toInt()

            if (newCount > 0) {
                fetchAndSaveNewPosts(sinceTimestamp, now, newCount, meta)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("FeedSyncWorker", "Error al sincronizar feed en background: ${e.message}", e)
            Result.success() // Retornamos success para evitar reintentos continuos si hay fallos de red
        }
    }

    private suspend fun fetchAndSaveNewPosts(sinceTimestamp: Long, now: Long, newCount: Int, meta: SyncMetadataEntity?) {
        val snapshot = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereGreaterThan("timestamp", Date(sinceTimestamp))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        val posts = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
        }

        if (posts.isNotEmpty()) {
            postDao.upsertPosts(posts.map { it.toEntity() })
        }

        // Guardar metadatos actualizados
        val newMeta = (meta ?: SyncMetadataEntity(key = "feed_global")).copy(
            lastSyncTimestamp = now
        )
        syncMetadataDao.upsert(newMeta)

        // Lanzar notificación local
        NotificationHelper.showNewPostsNotification(context, newCount)
    }
}
