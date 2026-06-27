package com.farbalapps.rinde.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.worker.UploadProfileWorker
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.repository.ProfileRepository
import com.farbalapps.rinde.util.ImageOptimizer

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldPath
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.*
import javax.inject.Inject
import com.farbalapps.rinde.util.CloudinaryHelper

import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.mapper.toDomain

import com.farbalapps.rinde.data.util.VotesMemoryCache

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileDao: ProfileDao,
    private val postDao: PostDao,
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val votesMemoryCache: VotesMemoryCache
) : ProfileRepository {

    private fun generateGenericName(userId: String): String {
        val hash = userId.hashCode().let { if (it < 0) -it else it }
        val suffix = hash.toString().takeLast(4).padStart(4, '0')
        return "user$suffix"
    }

    override fun getProfile(userId: String): Flow<Profile> {
        return profileDao.getProfile(userId).map { entity ->
            entity?.toDomainModel() ?: Profile(
                id = userId,
                name = generateGenericName(userId),
                isDummy = true
            )
        }
    }

    override fun getProfilePosts(userId: String): Flow<List<CommunityPost>> {
        android.util.Log.d("ProfileRepositoryImpl", "getProfilePosts observing Room for userId: $userId")
        
        // Lanzamos fetch asíncrono para poblar Room
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .whereEqualTo("isActive", true)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
                }
                
                if (posts.isNotEmpty()) {
                    val enriched = posts.map { post ->
                        val localPost = postDao.getPostById(post.id)
                        val cachedVote = votesMemoryCache.getVote(post.id)
                        val finalVote = cachedVote ?: localPost?.myVoteValue ?: 0
                        post.copy(myVoteValue = finalVote).toEntity()
                    }
                    postDao.upsertPosts(enriched)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileRepositoryImpl", "Error performing initial fetch of profile posts: ${e.message}")
            }
        }

        // Room is the SSOT:
        return postDao.getPostsByAuthorId(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }


    override suspend fun syncProfile(userId: String) {
        android.util.Log.i("ProfileRepositoryImpl", "🚀 INICIANDO SYNC FORZADO (SERVER) para: $userId")
        
        // Forzamos la lectura desde el servidor para evitar datos viejos de la caché local
        val snapshot = firestore.collection("users").document(userId)
            .get(com.google.firebase.firestore.Source.SERVER).await()
        val data = snapshot.data

        android.util.Log.d("ProfileRepositoryImpl", "📡 Datos recibidos de Firestore para $userId: $data")

        val profile = if (data != null) {
            val resolvedName = (data["name"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["displayName"] as? String)?.takeIf { it.isNotBlank() }
                ?: generateGenericName(userId)

            val needsPatch = data["name"] == null || data["followersCount"] == null
            if (needsPatch) {
                android.util.Log.w("ProfileRepositoryImpl", "⚠️ Parcheando documento legacy para $userId")
                val patch = mutableMapOf<String, Any>(
                    "name" to resolvedName,
                    "followersCount" to ((data["followersCount"] as? Number)?.toInt() ?: 0),
                    "followingCount" to ((data["followingCount"] as? Number)?.toInt() ?: 0),
                    "postsCount" to ((data["postsCount"] as? Number)?.toInt() ?: 0),
                    "rating" to ((data["rating"] as? Number)?.toDouble() ?: 0.0),
                    "reviewsCount" to ((data["reviewsCount"] as? Number)?.toInt() ?: 0),
                    "isPrivate" to (data["isPrivate"] as? Boolean ?: false)
                )
                data["email"]?.let { patch["email"] = it as Any }
                data["photoUrl"]?.let { patch["photoUrl"] = it as Any }
                firestore.collection("users").document(userId)
                    .set(patch, SetOptions.merge()).await()
            }

            val interestsList = (data["interests"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val zonasDeCazaList = (data["zonasDeCaza"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            Profile(
                id = userId,
                name = resolvedName,
                email = data["email"] as? String ?: "",
                photoUrl = data["photoUrl"] as? String,
                followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
                reviewsCount = (data["reviewsCount"] as? Number)?.toInt() ?: 0,
                isPrivate = data["isPrivate"] as? Boolean ?: false,
                isDummy = false,
                uploadStatus = data["uploadStatus"] as? String,
                interests = interestsList,
                zonasDeCaza = zonasDeCazaList
            )
        } else {
            android.util.Log.w("ProfileRepositoryImpl", "❓ No existe documento en Firestore para $userId — Creando uno nuevo")
            val newName = generateGenericName(userId)
            val initialData = mapOf(
                "name" to newName,
                "email" to "",
                "photoUrl" to null,
                "followersCount" to 0,
                "followingCount" to 0,
                "postsCount" to 0,
                "rating" to 0.0,
                "reviewsCount" to 0,
                "isPrivate" to false,
                "interests" to emptyList<String>(),
                "zonasDeCaza" to emptyList<String>()
            ).filterValues { it != null }
            
            firestore.collection("users").document(userId).set(initialData, SetOptions.merge()).await()
            Profile(id = userId, name = newName, isDummy = false)
        }

        profileDao.insertProfile(profile.toEntity())
        android.util.Log.i("ProfileRepositoryImpl", "✅ Perfil sincronizado y guardado en Room para $userId")
    }

    override suspend fun followUser(myUserId: String, targetUserId: String): Result<Unit> = runCatching {
        val relationshipId = "${myUserId}_$targetUserId"

        // Batch atómico: relación raíz + subcollecciones following/followers + contadores
        // Usamos batch (no transaction) porque no necesitamos leer antes de escribir
        firestore.runBatch { batch ->
            // 1. Relación raíz (compatibilidad con código existente)
            batch.set(
                firestore.collection("relationships").document(relationshipId),
                mapOf(
                    "followerId" to myUserId,
                    "followedId" to targetUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
            // 2. Subcollección following del que sigue (para fan-out y lookup rápido)
            batch.set(
                firestore.collection("users").document(myUserId)
                    .collection("following").document(targetUserId),
                mapOf("followedAt" to FieldValue.serverTimestamp())
            )
            // 3. Subcollección followers del seguido (para Cloud Functions futuras + lookup)
            batch.set(
                firestore.collection("users").document(targetUserId)
                    .collection("followers").document(myUserId),
                mapOf("followedAt" to FieldValue.serverTimestamp())
            )
            // 4. Contadores desnormalizados en el perfil
            batch.update(
                firestore.collection("users").document(myUserId),
                "followingCount", FieldValue.increment(1)
            )
            batch.update(
                firestore.collection("users").document(targetUserId),
                "followersCount", FieldValue.increment(1)
            )
        }.await()
    }

    override suspend fun unfollowUser(myUserId: String, targetUserId: String): Result<Unit> = runCatching {
        val relationshipId = "${myUserId}_$targetUserId"

        firestore.runBatch { batch ->
            // 1. Eliminar relación raíz
            batch.delete(firestore.collection("relationships").document(relationshipId))
            // 2. Eliminar de la subcollección following
            batch.delete(
                firestore.collection("users").document(myUserId)
                    .collection("following").document(targetUserId)
            )
            // 3. Eliminar de la subcollección followers
            batch.delete(
                firestore.collection("users").document(targetUserId)
                    .collection("followers").document(myUserId)
            )
            // 4. Decrementar contadores
            batch.update(
                firestore.collection("users").document(myUserId),
                "followingCount", FieldValue.increment(-1)
            )
            batch.update(
                firestore.collection("users").document(targetUserId),
                "followersCount", FieldValue.increment(-1)
            )
        }.await()
    }

    override fun isFollowing(myUserId: String, targetUserId: String): Flow<Boolean> = callbackFlow {
        // Lookup directo en subcollección following — O(1) por document ID, sin index
        val listener = firestore.collection("users").document(myUserId)
            .collection("following").document(targetUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fallback: no cerramos el flow, emitimos false
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot != null && snapshot.exists())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(userId: String, name: String, photoUrl: String?): Result<Unit> = runCatching {
        if (userId.isBlank()) throw Exception("User ID is required")
        
        var localOptimizedFilePath: String? = null
        val validatedName = if (name.isBlank()) generateGenericName(userId) else name

        if (photoUrl != null && (photoUrl.startsWith("content://") || photoUrl.startsWith("file://"))) {
            val optimizedFile = ImageOptimizer.optimizeImage(context, Uri.parse(photoUrl))
            localOptimizedFilePath = optimizedFile?.absolutePath
        }

        val currentProfileEntity = profileDao.getProfile(userId).firstOrNull()
        // photoUrl será: localOptimizedFilePath (si es nueva), photoUrl (si se mantiene o se elimina con null)
        val targetPhotoUrl = localOptimizedFilePath ?: photoUrl
        
        val updatedProfile = Profile(
            id = userId,
            name = validatedName,
            email = currentProfileEntity?.email ?: "",
            photoUrl = targetPhotoUrl,
            followersCount = currentProfileEntity?.followersCount ?: 0,
            followingCount = currentProfileEntity?.followingCount ?: 0,
            postsCount = currentProfileEntity?.postsCount ?: 0,
            isPrivate = currentProfileEntity?.isPrivate ?: false
        )
        profileDao.insertProfile(updatedProfile.toEntity())

        // LANZAR SUBIDA INMEDIATA
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Obtener imagen actual de Firestore para borrarla
                val userDoc = firestore.collection("users").document(userId).get().await()
                val currentPhotoUrl = userDoc.getString("photoUrl")

                firestore.collection("users").document(userId)
                    .set(mapOf("uploadStatus" to "Subiendo a Cloudinary..."), SetOptions.merge())
                    .await()

                var finalPhotoUrl: String? = targetPhotoUrl
                var isDeletion = photoUrl == null && localOptimizedFilePath == null

                if (isDeletion) {
                    // Borrar la anterior de Cloudinary si existía
                    currentPhotoUrl?.let { oldUrl ->
                        if (oldUrl.contains("cloudinary.com")) {
                            android.util.Log.d("ProfileRepositoryImpl", "🗑️ Borrando imagen anterior por eliminación...")
                            CloudinaryHelper.deleteImage(oldUrl)
                        }
                    }
                    finalPhotoUrl = null
                } else if (localOptimizedFilePath != null) {
                    val file = java.io.File(localOptimizedFilePath)
                    if (file.exists()) {
                        // Borrar la anterior si es de Cloudinary
                        currentPhotoUrl?.let { oldUrl ->
                            if (oldUrl.contains("cloudinary.com")) {
                                android.util.Log.d("ProfileRepositoryImpl", "🗑️ Borrando imagen anterior...")
                                CloudinaryHelper.deleteImage(oldUrl)
                            }
                        }

                        // Subir la nueva
                        finalPhotoUrl = CloudinaryHelper.uploadImage(localOptimizedFilePath, "USERS")
                        file.delete()
                    }
                }

                // 2. Actualizar Firestore con la nueva URL (o null)
                val updates = mutableMapOf<String, Any?>(
                    "name" to validatedName,
                    "uploadStatus" to "Subida completada ✅",
                    "photoUrl" to finalPhotoUrl
                )
                
                try {
                    FirebaseAuth.getInstance().currentUser?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setPhotoUri(finalPhotoUrl?.let { Uri.parse(it) })
                            .build()
                    )?.await()
                    android.util.Log.d("ProfileRepositoryImpl", "✅ FirebaseAuth profile photo updated.")
                } catch (e: Exception) {
                    android.util.Log.e("ProfileRepositoryImpl", "Error updating Firebase Auth photo: ${e.message}")
                }
                
                firestore.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

                // Propagar cambios a todos los posts del autor
                try {
                    android.util.Log.d("ProfileRepositoryImpl", "🔄 Propagando cambios de autor a sus posts...")
                    val postsSnapshot = firestore.collection("posts")
                        .whereEqualTo("authorId", userId)
                        .get().await()
                    
                    if (!postsSnapshot.isEmpty) {
                        firestore.runBatch { batch ->
                            postsSnapshot.documents.forEach { doc ->
                                val postRef = firestore.collection("posts").document(doc.id)
                                val postUpdates = mutableMapOf<String, Any?>(
                                    "authorName" to validatedName,
                                    "authorPhotoUrl" to finalPhotoUrl
                                )
                                batch.update(postRef, postUpdates)
                            }
                        }.await()
                        android.util.Log.d("ProfileRepositoryImpl", "✅ Propagación exitosa a ${postsSnapshot.size()} posts")
                    }
                    
                    // Actualizar el caché local para evitar inconsistencias instantáneas en UI
                    postDao.updateAuthorProfile(userId, validatedName, finalPhotoUrl)
                    android.util.Log.d("ProfileRepositoryImpl", "✅ Local Room cache updated for author posts.")
                    
                } catch (pe: Exception) {
                    android.util.Log.e("ProfileRepositoryImpl", "⚠️ Error al propagar cambios a posts", pe)
                }

                // 3. Sincronizar localmente
                syncProfile(userId)

                
            } catch (e: Exception) {
                android.util.Log.e("ProfileRepositoryImpl", "❌ Error en subida: ${e.message}")
                firestore.collection("users").document(userId)
                    .set(mapOf("uploadStatus" to "Error: ${e.localizedMessage}"), SetOptions.merge())
                    .await()
            }
        }
        
        Result.success(Unit)
    }

    // --- New Features Implementation ---

    override suspend fun updatePrivacy(userId: String, isPrivate: Boolean): Result<Unit> = runCatching {
        // Optimistic local update
        val current = profileDao.getProfile(userId).firstOrNull()
        current?.let {
            profileDao.insertProfile(it.copy(isPrivate = isPrivate))
        }

        firestore.collection("users").document(userId)
            .set(mapOf("isPrivate" to isPrivate), SetOptions.merge())
            .await()
    }

    override suspend fun toggleSavePost(userId: String, postId: String, save: Boolean): Result<Unit> = runCatching {
        val savedRef = firestore.collection("users").document(userId)
            .collection("saved_posts").document(postId)

        if (save) {
            // Solo guardamos la referencia al postId + timestamp
            // La hydration (obtener datos reales del post) se hace en getSavedProfilePosts
            savedRef.set(mapOf(
                "postId" to postId,
                "savedAt" to FieldValue.serverTimestamp()
            )).await()
        } else {
            savedRef.delete().await()
        }
    }

    override suspend fun blockUser(userId: String, targetUserId: String): Result<Unit> = runCatching {
        // Logic: 
        // 1. Add to blocked collection
        // 2. Automatically unfollow (both ways)
        
        val blockRef = firestore.collection("users").document(userId)
            .collection("blocked_users").document(targetUserId)
        
        firestore.runTransaction { transaction ->
            // 1. Block
            transaction.set(blockRef, mapOf(
                "blockedId" to targetUserId,
                "blockedAt" to FieldValue.serverTimestamp()
            ))
            
            // Note: Transactions cannot call other suspend functions easily 
            // but we can manually perform the unfollow logic here.
        }.await()
        
        // 2. Auto-unfollow (Sequential after transaction to ensure safety)
        unfollowUser(userId, targetUserId) // I unfollow them
        unfollowUser(targetUserId, userId) // They unfollow me
    }

    override suspend fun unblockUser(userId: String, targetUserId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .collection("blocked_users").document(targetUserId)
            .delete()
            .await()
    }

    override suspend fun clearUploadStatus(userId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .update("uploadStatus", "")
            .await()
        
        // Actualización local para evitar el parpadeo en la UI
        val current = profileDao.getProfile(userId).firstOrNull()
        current?.let {
            profileDao.insertProfile(it.copy(uploadStatus = ""))
        }
    }

    override fun getSavedProfilePosts(userId: String): Flow<List<CommunityPost>> = callbackFlow {
        // Escucha la colección de referencias guardadas
        val listener = firestore.collection("users").document(userId)
            .collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Hydration: obtener los posts reales desde la colección posts
                launch {
                    try {
                        val posts = postIds.chunked(30).flatMap { chunk ->
                            firestore.collection("posts")
                                .whereIn(FieldPath.documentId(), chunk)
                                .whereEqualTo("isActive", true)
                                .get().await()
                                .documents.mapNotNull { doc ->
                                    doc.toObject(CommunityPostDto::class.java)
                                        ?.copy(id = doc.id)
                                        ?.toDomain()
                                }
                        }
                        // Mantener el orden de savedAt
                        val ordered = postIds.mapNotNull { id -> posts.find { it.id == id } }
                        trySend(ordered)
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileRepositoryImpl", "❌ Error hydrating saved posts", e)
                        trySend(emptyList())
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getBlockedUsers(userId: String): Flow<List<Profile>> = callbackFlow {
        val listener = firestore.collection("users").document(userId).collection("blocked_users")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val blockedIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (blockedIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                firestore.collection("users")
                    .whereIn(FieldPath.documentId(), blockedIds.take(30))
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        val users = userSnapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            Profile(
                                id = doc.id,
                                name = data["name"] as? String ?: "Usuario",
                                photoUrl = data["photoUrl"] as? String
                            )
                        }
                        trySend(users)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                    }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateInterests(userId: String, interests: List<String>): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .set(mapOf("interests" to interests), SetOptions.merge())
            .await()
        syncProfile(userId)
    }

    override suspend fun updateZonasDeCaza(userId: String, zonas: List<String>): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .set(mapOf("zonasDeCaza" to zonas), SetOptions.merge())
            .await()
        syncProfile(userId)
    }
}

