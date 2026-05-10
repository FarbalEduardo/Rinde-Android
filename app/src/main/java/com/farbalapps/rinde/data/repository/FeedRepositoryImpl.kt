package com.farbalapps.rinde.data.repository

import android.content.Context
import android.net.Uri
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.util.ImageOptimizer
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.farbalapps.rinde.data.worker.CreatePostWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class FeedRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) : FeedRepository {

    override fun getGlobalFeed(lastPostId: String?): Flow<List<CommunityPost>> = callbackFlow {
        // Nota: el índice simple isActive+timestamp generalmente se auto-crea,
        // pero si falla, aplicar filtro client-side como fallback.
        var query = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Busca más para compensar el filtro client-side

        if (lastPostId != null) {
            val lastDoc = firestore.collection("posts").document(lastPostId).get().await()
            query = query.startAfter(lastDoc)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FeedRepositoryImpl", "Firestore listener error in getGlobalFeed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            // Filtro client-side para isActive
            val posts = snapshot?.documents
                ?.mapNotNull { it.toObject(CommunityPost::class.java) }
                ?.filter { it.isActive }
                ?.take(20)
                ?: emptyList()
            trySend(posts)
        }
        awaitClose { listener.remove() }
    }

    override fun getFollowingFeed(userId: String, lastPostId: String?): Flow<List<CommunityPost>> = callbackFlow {
        // Primero obtenemos a quién sigue el usuario
        val relationshipsListener = firestore.collection("relationships")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { relSnapshot, relError ->
                if (relError != null) {
                    android.util.Log.e("FeedRepositoryImpl", "Firestore listener error in getFollowingFeed", relError)
                    return@addSnapshotListener
                }
                
                val followedIds = relSnapshot?.documents?.mapNotNull { it.getString("followedId") } ?: emptyList()
                
                if (followedIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Firestore IN query tiene un limite de 30 items.
                val cappedIds = followedIds.take(30)

                var query = firestore.collection("posts")
                    .whereEqualTo("isActive", true)
                    .whereIn("authorId", cappedIds)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)

                if (lastPostId != null) {
                    launch {
                        try {
                            val lastDoc = firestore.collection("posts").document(lastPostId).get().await()
                            val finalQuery = query.startAfter(lastDoc)
                            finalQuery.get().addOnSuccessListener { postSnapshot ->
                                val posts = postSnapshot.documents.mapNotNull { it.toObject(CommunityPost::class.java) }
                                trySend(posts)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FeedRepositoryImpl", "Error fetching cursor doc", e)
                        }
                    }
                } else {
                    query.get().addOnSuccessListener { postSnapshot ->
                        val posts = postSnapshot.documents.mapNotNull { it.toObject(CommunityPost::class.java) }
                        trySend(posts)
                    }
                }
            }
        awaitClose { relationshipsListener.remove() }
    }

    override fun getSavedPosts(userId: String): Flow<List<CommunityPost>> = callbackFlow {
        // Ordenar por 'savedAt' (ServerTimestamp que se graba al guardar)
        val listener = firestore.collection("users").document(userId).collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FeedRepositoryImpl", "Firestore listener error in getSavedPosts", error)
                    return@addSnapshotListener
                }
                
                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Usar FieldPath.documentId() para filtrar por document ID en Firestore
                firestore.collection("posts")
                    .whereIn(FieldPath.documentId(), postIds.take(30))
                    .get()
                    .addOnSuccessListener { postSnapshot ->
                        val posts = postSnapshot.documents.mapNotNull { it.toObject(CommunityPost::class.java) }
                        trySend(posts)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FeedRepositoryImpl", "Error fetching saved posts by IDs", e)
                        trySend(emptyList())
                    }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit> = runCatching {
        // 1. Optimizar imágenes localmente de inmediato
        val localPaths = photoUris.mapNotNull { uriString ->
            val uri = Uri.parse(uriString)
            ImageOptimizer.optimizeImage(context, uri)?.absolutePath
        }.toTypedArray()

        // 2. Encolar el Worker para subida en segundo plano
        val uploadWorkRequest = OneTimeWorkRequestBuilder<CreatePostWorker>()
            .setInputData(workDataOf(
                "authorId" to post.authorId,
                "authorName" to post.authorName,
                "title" to post.title,
                "descriptionLong" to post.descriptionLong,
                "category" to post.category,
                "locationName" to post.location.name,
                "localFilePaths" to localPaths
            ))
            .build()
        
        workManager.enqueue(uploadWorkRequest)
    }

    override suspend fun toggleLike(userId: String, postId: String): Result<Unit> = runCatching {
        val postRef = firestore.collection("posts").document(postId)
        // Lógica simplificada: en producción usaríamos una subcolección 'likes' y Sharding para escalabilidad
        postRef.update("likes", FieldValue.increment(1)).await()
    }

    override suspend fun toggleSave(userId: String, postId: String): Result<Unit> = runCatching {
        val savedRef = firestore.collection("users").document(userId).collection("saved_posts").document(postId)
        val doc = savedRef.get().await()
        if (doc.exists()) {
            savedRef.delete().await()
        } else {
            // Usar 'savedAt' con ServerTimestamp para consistencia con ProfileRepository y la query de getSavedPosts
            savedRef.set(mapOf("savedAt" to FieldValue.serverTimestamp())).await()
        }
    }
}
