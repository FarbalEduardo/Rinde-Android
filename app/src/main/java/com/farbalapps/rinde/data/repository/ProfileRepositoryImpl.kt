package com.farbalapps.rinde.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileDao: ProfileDao,
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
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

    override fun getProfilePosts(userId: String): Flow<List<CommunityPost>> = callbackFlow {
        // Nota: Se eliminó el filtro .whereEqualTo("isActive", true) para evitar
        // requerir un índice compuesto en Firestore.
        val query = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("ProfileRepositoryImpl", "Error fetching profile posts", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val posts = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(CommunityPostDto::class.java)?.toDomain()?.takeIf { it.isActive }
            } ?: emptyList()
            
            trySend(posts)
        }
        awaitClose { listener.remove() }
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
                uploadStatus = data["uploadStatus"] as? String
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
                "isPrivate" to false
            ).filterValues { it != null }
            
            firestore.collection("users").document(userId).set(initialData, SetOptions.merge()).await()
            Profile(id = userId, name = newName, isDummy = false)
        }

        profileDao.insertProfile(profile.toEntity())
        android.util.Log.i("ProfileRepositoryImpl", "✅ Perfil sincronizado y guardado en Room para $userId")
    }

    override suspend fun followUser(myUserId: String, targetUserId: String): Result<Unit> = runCatching {
        val relationshipId = "${myUserId}_$targetUserId"
        val relationshipRef = firestore.collection("relationships").document(relationshipId)
        
        firestore.runTransaction { transaction ->
            transaction.set(relationshipRef, mapOf(
                "followerId" to myUserId,
                "followedId" to targetUserId,
                "timestamp" to FieldValue.serverTimestamp()
            ))
            
            val myUserRef = firestore.collection("users").document(myUserId)
            val targetUserRef = firestore.collection("users").document(targetUserId)
            
            transaction.update(myUserRef, "followingCount", FieldValue.increment(1))
            transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
        }.await()
    }

    override suspend fun unfollowUser(myUserId: String, targetUserId: String): Result<Unit> = runCatching {
        val relationshipId = "${myUserId}_$targetUserId"
        val relationshipRef = firestore.collection("relationships").document(relationshipId)
        
        firestore.runTransaction { transaction ->
            transaction.delete(relationshipRef)
            
            val myUserRef = firestore.collection("users").document(myUserId)
            val targetUserRef = firestore.collection("users").document(targetUserId)
            
            transaction.update(myUserRef, "followingCount", FieldValue.increment(-1))
            transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
        }.await()
    }

    override fun isFollowing(myUserId: String, targetUserId: String): Flow<Boolean> = callbackFlow {
        val relationshipId = "${myUserId}_$targetUserId"
        val listener = firestore.collection("relationships").document(relationshipId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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
        val updatedProfile = Profile(
            id = userId,
            name = validatedName,
            email = currentProfileEntity?.email ?: "",
            photoUrl = localOptimizedFilePath ?: photoUrl ?: currentProfileEntity?.photoUrl,
            followersCount = currentProfileEntity?.followersCount ?: 0,
            followingCount = currentProfileEntity?.followingCount ?: 0,
            postsCount = currentProfileEntity?.postsCount ?: 0,
            isPrivate = currentProfileEntity?.isPrivate ?: false
        )
        profileDao.insertProfile(updatedProfile.toEntity())

        // LANZAR SUBIDA INMEDIATA (Volvemos a Coroutine para asegurar ejecución inmediata)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Obtener imagen actual de Firestore para borrarla
                val userDoc = firestore.collection("users").document(userId).get().await()
                val currentPhotoUrl = userDoc.getString("photoUrl")

                firestore.collection("users").document(userId)
                    .set(mapOf("uploadStatus" to "Subiendo a Cloudinary..."), SetOptions.merge())
                    .await()

                var finalPhotoUrl: String? = null
                if (localOptimizedFilePath != null) {
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

                // 2. Actualizar Firestore con la nueva URL
                val updates = mutableMapOf<String, Any>(
                    "name" to validatedName,
                    "uploadStatus" to "Subida completada ✅"
                )
                finalPhotoUrl?.let { updates["photoUrl"] = it }
                
                firestore.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

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
        val listener = firestore.collection("users").document(userId).collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
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

                firestore.collection("posts")
                    .whereIn(FieldPath.documentId(), postIds.take(30))
                    .get()
                    .addOnSuccessListener { postSnapshot ->
                        val posts = postSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(CommunityPostDto::class.java)?.toDomain()
                        }
                        trySend(posts)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                    }
            }
        awaitClose { listener.remove() }
    }
}
