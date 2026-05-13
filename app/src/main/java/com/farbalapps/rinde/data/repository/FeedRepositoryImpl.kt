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
import com.google.firebase.database.FirebaseDatabase
import com.farbalapps.rinde.data.worker.CreatePostWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import kotlinx.coroutines.flow.map
import java.util.Date

import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.mapper.toDomain

class FeedRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val workManager: WorkManager,
    private val postDao: PostDao
) : FeedRepository {

    override fun getGlobalFeed(lastPostId: String?): Flow<List<CommunityPost>> {
        // 1. Iniciar sincronización en segundo plano (Incremental)
        kotlinx.coroutines.GlobalScope.launch {
            try {
                syncLatestPosts()
            } catch (e: Exception) {
                android.util.Log.e("FeedRepositoryImpl", "Error en sync incremental", e)
            }
        }

        // 2. Retornar el flujo desde Room (Fuente de Verdad)
        return postDao.getPosts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    private suspend fun syncLatestPosts() {
        val lastTimestamp = postDao.getLatestTimestamp() ?: 0L
        android.util.Log.d("FeedRepositoryImpl", "🔄 Sincronizando posts posteriores a: ${Date(lastTimestamp)}")

        val snapshot = firestore.collection("posts")
            .whereGreaterThan("timestamp", Date(lastTimestamp))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        if (!snapshot.isEmpty) {
            val newPosts = snapshot.documents.mapNotNull { it.toObject(CommunityPostDto::class.java)?.toDomain() }
            android.util.Log.i("FeedRepositoryImpl", "📥 Descargados ${newPosts.size} nuevos posts")
            postDao.insertPosts(newPosts.map { it.toEntity() })
        }
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
                                val posts = postSnapshot.documents.mapNotNull { it.toObject(CommunityPostDto::class.java)?.toDomain() }
                                trySend(posts)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FeedRepositoryImpl", "Error fetching cursor doc", e)
                        }
                    }
                } else {
                    query.get().addOnSuccessListener { postSnapshot ->
                        val posts = postSnapshot.documents.mapNotNull { it.toObject(CommunityPostDto::class.java)?.toDomain() }
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
                        val posts = postSnapshot.documents.mapNotNull { it.toObject(CommunityPostDto::class.java)?.toDomain() }
                        trySend(posts)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FeedRepositoryImpl", "Error fetching saved posts by IDs", e)
                        trySend(emptyList())
                    }
            }
        awaitClose { listener.remove() }
    }

    override fun getNearbyFeed(lat: Double, lon: Double, radiusKm: Double): Flow<List<CommunityPost>> = callbackFlow {
        // Aproximación de bounding box (1 grado ~ 111km)
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(lat)))

        val query = firestore.collection("posts")
            .whereGreaterThanOrEqualTo("location.latitude", lat - latDelta)
            .whereLessThanOrEqualTo("location.latitude", lat + latDelta)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val posts = snapshot?.documents?.mapNotNull { it.toObject(CommunityPostDto::class.java)?.toDomain() }
                ?.filter { post ->
                    // Filtro secundario para longitud y estado activo
                    post.isActive && 
                    post.location.longitude != null && 
                    post.location.longitude!! >= (lon - lonDelta) && 
                    post.location.longitude!! <= (lon + lonDelta)
                } ?: emptyList()

            trySend(posts.sortedByDescending { it.timestamp })
        }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit> = runCatching {
        android.util.Log.d("FeedRepositoryImpl", "🚀 Iniciando proceso de subida para: ${post.title}")
        
        // 1. Validación de imágenes (Obligatorio min 1, max 4)
        if (photoUris.isEmpty()) {
            throw Exception("Debes incluir al menos una imagen para tu reporte")
        }

        // 2. Optimizar imágenes localmente
        val localPaths = photoUris.mapNotNull { uriString ->
            val uri = Uri.parse(uriString)
            ImageOptimizer.optimizeImage(context, uri)?.absolutePath
        }.toTypedArray()

        if (localPaths.isEmpty()) {
            throw Exception("Error procesando las imágenes seleccionadas")
        }

        android.util.Log.d("FeedRepositoryImpl", "📸 Imágenes procesadas: ${localPaths.size}")

        // 2. Encolar Worker
        workManager.enqueue(OneTimeWorkRequestBuilder<CreatePostWorker>()
            .setInputData(workDataOf(
                "authorId" to post.authorId,
                "authorName" to post.authorName,
                "authorPhotoUrl" to post.authorPhotoUrl,
                "title" to post.title,
                "descriptionLong" to post.descriptionLong,
                "category" to post.category,
                "locationName" to post.location.name,
                "localFilePaths" to localPaths,
                
                // v3 fields
                "offerType" to post.offerType.name,
                "websiteName" to post.websiteName,
                "productLink" to post.productLink,
                "storeName" to post.storeName,
                "userReputationScore" to post.userReputationScore
            ))
            .build())
        
        android.util.Log.i("FeedRepositoryImpl", "✅ Worker de creación de post encolado con éxito")
    }



    override suspend fun toggleLike(userId: String, postId: String): Result<Unit> = runCatching {
        val likeRef = firestore.collection("posts").document(postId).collection("likes").document(userId)
        val postRef = firestore.collection("posts").document(postId)
        
        firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)
            if (likeSnapshot.exists()) {
                // Ya le dio like, lo quitamos
                transaction.delete(likeRef)
                transaction.update(postRef, "likes", FieldValue.increment(-1))
            } else {
                // No le ha dado like, lo agregamos
                transaction.set(likeRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
                transaction.update(postRef, "likes", FieldValue.increment(1))
            }
        }.await()
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

    override suspend fun toggleVote(userId: String, postId: String, voteValue: Int): Result<Unit> = runCatching {
        val voteRef = database.getReference("post_votes").child(postId).child(userId)
        val postRef = firestore.collection("posts").document(postId)
        
        val currentVote = voteRef.get().await().getValue(Int::class.java) ?: 0
        
        if (currentVote == voteValue) {
            // Toggle off (Quitar el voto)
            voteRef.removeValue().await()
            postRef.update("votesScore", FieldValue.increment(-voteValue.toLong())).await()
        } else {
            // Set/Change vote
            voteRef.setValue(voteValue).await()
            
            val increment = if (currentVote == 0) {
                voteValue.toLong()
            } else {
                // Cambió de Hot a Cold o viceversa
                (voteValue - currentVote).toLong()
            }
            postRef.update("votesScore", FieldValue.increment(increment)).await()
        }
    }

}
