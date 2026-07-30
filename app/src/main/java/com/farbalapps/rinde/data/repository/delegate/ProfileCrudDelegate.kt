package com.farbalapps.rinde.data.repository.delegate

import android.content.Context
import android.net.Uri
import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.util.CloudinaryHelper
import com.farbalapps.rinde.util.ImageOptimizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileCrudDelegate @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileDao: ProfileDao,
    @ApplicationContext private val context: Context
) {
    fun generateGenericName(userId: String): String {
        val hash = userId.hashCode().let { if (it < 0) -it else it }
        val suffix = hash.toString().takeLast(4).padStart(4, '0')
        return "user$suffix"
    }

    fun getProfile(userId: String): Flow<Profile> {
        return profileDao.getProfile(userId).map { entity ->
            entity?.toDomainModel() ?: Profile(
                id = userId,
                name = generateGenericName(userId),
                isDummy = true
            )
        }
    }

    suspend fun syncProfile(userId: String) {
        android.util.Log.i("ProfileCrudDelegate", "🚀 INICIANDO SYNC FORZADO (SERVER) para: $userId")
        val snapshot = firestore.collection("users").document(userId)
            .get(com.google.firebase.firestore.Source.SERVER).await()
        val data = snapshot.data

        val profile = if (data != null) {
            val resolvedName = (data["name"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["displayName"] as? String)?.takeIf { it.isNotBlank() }
                ?: generateGenericName(userId)

            val authUser = FirebaseAuth.getInstance().currentUser
            val firebaseAuthPhotoUrl = if (authUser?.uid == userId) authUser.photoUrl?.toString() else null
            val rawPhotoUrl = ((data["photoUrl"] as? String) ?: (data["photoURL"] as? String))?.takeIf { it.isNotBlank() }
            val resolvedPhotoUrl = rawPhotoUrl ?: firebaseAuthPhotoUrl

            val needsPatch = data["name"] == null || data["followersCount"] == null || data["commentsCount"] == null || (data["photoUrl"] == null && resolvedPhotoUrl != null)
            if (needsPatch) {
                val patch = mutableMapOf<String, Any>(
                    "name" to resolvedName,
                    "followersCount" to ((data["followersCount"] as? Number)?.toInt() ?: 0),
                    "followingCount" to ((data["followingCount"] as? Number)?.toInt() ?: 0),
                    "postsCount" to ((data["postsCount"] as? Number)?.toInt() ?: 0),
                    "commentsCount" to ((data["commentsCount"] as? Number)?.toInt() ?: 0),
                    "rating" to ((data["rating"] as? Number)?.toDouble() ?: 0.0),
                    "reviewsCount" to ((data["reviewsCount"] as? Number)?.toInt() ?: 0),
                    "isPrivate" to (data["isPrivate"] as? Boolean ?: false)
                )
                data["email"]?.let { patch["email"] = it as Any }
                resolvedPhotoUrl?.let { patch["photoUrl"] = it }
                firestore.collection("users").document(userId)
                    .set(patch, SetOptions.merge()).await()
            }

            val interestsList = (data["interests"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val zonasDeCazaList = (data["zonasDeCaza"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            Profile(
                id = userId,
                name = resolvedName,
                email = data["email"] as? String ?: "",
                photoUrl = resolvedPhotoUrl,
                followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0,
                rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
                reviewsCount = (data["reviewsCount"] as? Number)?.toInt() ?: 0,
                isPrivate = data["isPrivate"] as? Boolean ?: false,
                isDummy = false,
                uploadStatus = data["uploadStatus"] as? String,
                interests = interestsList,
                zonasDeCaza = zonasDeCazaList
            )
        } else {
            val newName = generateGenericName(userId)
            val authUser = FirebaseAuth.getInstance().currentUser
            val firebaseAuthPhotoUrl = if (authUser?.uid == userId) authUser.photoUrl?.toString() else null

            val initialData = mapOf(
                "name" to newName,
                "email" to "",
                "photoUrl" to firebaseAuthPhotoUrl,
                "followersCount" to 0,
                "followingCount" to 0,
                "postsCount" to 0,
                "commentsCount" to 0,
                "rating" to 0.0,
                "reviewsCount" to 0,
                "isPrivate" to false,
                "interests" to emptyList<String>(),
                "zonasDeCaza" to emptyList<String>()
            )
            firestore.collection("users").document(userId).set(initialData, SetOptions.merge()).await()
            Profile(id = userId, name = newName, photoUrl = firebaseAuthPhotoUrl, isDummy = false)
        }

        profileDao.insertProfile(profile.toEntity())
    }

    suspend fun updateProfile(userId: String, name: String, photoUrl: String?): Result<Unit> = runCatching {
        if (userId.isBlank()) throw Exception("User ID is required")
        
        var localOptimizedFilePath: String? = null
        val validatedName = if (name.isBlank()) generateGenericName(userId) else name

        if (photoUrl != null && (photoUrl.startsWith("content://") || photoUrl.startsWith("file://"))) {
            val optimizedFile = ImageOptimizer.optimizeImage(context, Uri.parse(photoUrl))
            localOptimizedFilePath = optimizedFile?.absolutePath
        }

        val currentProfileEntity = profileDao.getProfile(userId).firstOrNull()
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users").document(userId)
                    .set(mapOf("uploadStatus" to "Subiendo a Cloudinary..."), SetOptions.merge())
                    .await()

                var finalPhotoUrl: String? = targetPhotoUrl
                val isDeletion = photoUrl == null && localOptimizedFilePath == null

                if (isDeletion) {
                    finalPhotoUrl = null
                } else if (localOptimizedFilePath != null) {
                    val file = java.io.File(localOptimizedFilePath)
                    if (file.exists()) {
                        finalPhotoUrl = CloudinaryHelper.uploadImage(localOptimizedFilePath, "USERS", userId)
                        val currentEntity = profileDao.getProfile(userId).firstOrNull()
                        if (currentEntity != null && !finalPhotoUrl.isNullOrBlank()) {
                            profileDao.insertProfile(currentEntity.copy(photoUrl = finalPhotoUrl, uploadStatus = "Subida completada ✅"))
                        }
                        file.delete()
                    }
                }

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
                    android.util.Log.d("ProfileCrudDelegate", "✅ FirebaseAuth profile photo updated.")
                } catch (e: Exception) {
                    android.util.Log.e("ProfileCrudDelegate", "Error updating Firebase Auth photo: ${e.message}")
                }

                firestore.collection("users").document(userId).set(updates, SetOptions.merge()).await()

                // Propagar a sus posts
                try {
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
                    }
                } catch (pe: Exception) {
                    android.util.Log.e("ProfileCrudDelegate", "⚠️ Error al propagar cambios a posts", pe)
                }

                syncProfile(userId)
                
            } catch (e: Exception) {
                firestore.collection("users").document(userId)
                    .set(mapOf("uploadStatus" to "Error: ${e.localizedMessage}"), SetOptions.merge())
                    .await()
            }
        }
    }

    suspend fun clearUploadStatus(userId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .update("uploadStatus", "")
            .await()
        
        val current = profileDao.getProfile(userId).firstOrNull()
        current?.let {
            profileDao.insertProfile(it.copy(uploadStatus = ""))
        }
    }
}
