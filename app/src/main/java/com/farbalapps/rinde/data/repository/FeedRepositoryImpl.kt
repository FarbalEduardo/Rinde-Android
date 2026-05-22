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

    override fun getGlobalFeed(lastPostId: String?): Flow<List<CommunityPost>> = callbackFlow {
        android.util.Log.d("FeedRepositoryImpl", "🛰️ Iniciando listener de tiempo real para Global Feed")
        
        val query = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)

        val snapshotListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FeedRepositoryImpl", "❌ Error en listener global", error)
                return@addSnapshotListener
            }

            snapshot?.documentChanges?.forEach { change ->
                val postDto = change.document.toObject(CommunityPostDto::class.java)
                val post = postDto?.toDomain() ?: return@forEach

                launch {
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            postDao.insertPosts(listOf(post.toEntity()))
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            android.util.Log.i("FeedRepositoryImpl", "🗑️ Post eliminado detectado: ${post.id}")
                            postDao.deletePostById(post.id)
                        }
                    }
                }
            }
        }

        // Emitir cambios desde Room (Fuente de Verdad única para la UI)
        val roomCollectionJob = launch {
            postDao.getPosts().collect { entities ->
                trySend(entities.map { it.toDomainModel() })
            }
        }

        awaitClose {
            android.util.Log.d("FeedRepositoryImpl", "🔌 Cerrando listener de Global Feed")
            snapshotListener.remove()
            roomCollectionJob.cancel()
        }
    }

    override fun getFollowingFeed(userId: String, lastPostId: String?): Flow<List<CommunityPost>> = callbackFlow {
        android.util.Log.d("FeedRepositoryImpl", "🛰️ Iniciando listener Following Feed para: $userId")
        
        // 1. Listen to followed users
        val relationshipsListener = firestore.collection("relationships")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { relSnapshot, relError ->
                if (relError != null) return@addSnapshotListener
                
                val followedIds = relSnapshot?.documents?.mapNotNull { it.getString("followedId") } ?: emptyList()
                
                if (followedIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // 2. Listen to posts from those users (Limit to 30 following for Firestore IN query)
                val postsQuery = firestore.collection("posts")
                    .whereEqualTo("isActive", true)
                    .whereIn("authorId", followedIds.take(30))
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(30)

                val postsListener = postsQuery.addSnapshotListener { postSnapshot, postError ->
                    if (postError != null) return@addSnapshotListener
                    
                    val posts = postSnapshot?.documents?.mapNotNull { 
                        it.toObject(CommunityPostDto::class.java)?.toDomain() 
                    } ?: emptyList()
                    
                    trySend(posts)
                }
                
                // Cleanup nested listener on next update
            }
            
        awaitClose { 
            android.util.Log.d("FeedRepositoryImpl", "🔌 Cerrando listener Following Feed")
            relationshipsListener.remove() 
        }
    }

    override fun getSavedPosts(userId: String): Flow<List<CommunityPost>> = callbackFlow {
        android.util.Log.d("FeedRepositoryImpl", "🛰️ Iniciando listener Saved Posts para: $userId")

        val listener = firestore.collection("users").document(userId).collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Listen to the actual posts to detect deletions or updates
                val postsQuery = firestore.collection("posts")
                    .whereIn(FieldPath.documentId(), postIds.take(30))

                val postsListener = postsQuery.addSnapshotListener { postSnapshot, postError ->
                    if (postError != null) return@addSnapshotListener
                    
                    val posts = postSnapshot?.documents?.mapNotNull { 
                        it.toObject(CommunityPostDto::class.java)?.toDomain() 
                    } ?: emptyList()
                    
                    // Re-sort because whereIn doesn't preserve order of postIds
                    val sortedPosts = posts.sortedByDescending { it.timestamp }
                    trySend(sortedPosts)
                }
            }
        awaitClose { 
            android.util.Log.d("FeedRepositoryImpl", "🔌 Cerrando listener Saved Posts")
            listener.remove() 
        }
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
