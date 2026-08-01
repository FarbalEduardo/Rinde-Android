package com.farbalapps.rinde.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.util.CloudinaryHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File

@HiltWorker
class UploadProfileWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val profileDao: ProfileDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val name = inputData.getString("name") ?: ""
        val localFilePath = inputData.getString("localFilePath")
        
        android.util.Log.d("UploadProfileWorker", "👷 Worker iniciado para $userId (Cloudinary REST)")
        
        return try {
            firestore.collection("users").document(userId)
                .update("uploadStatus", "Subiendo imagen a Cloudinary...")
                .await()

            val finalPhotoUrl = uploadImageToCloudinary(userId, localFilePath)
            updateFirestore(userId, name, finalPhotoUrl)
            syncLocalRoom(userId)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("UploadProfileWorker", "❌ Error en Worker: ${e.message}")
            e.printStackTrace()
            try {
                firestore.collection("users").document(userId)
                    .update("uploadStatus", "Error: ${e.localizedMessage}")
                    .await()
            } catch (inner: Exception) { /* ignore */ }
            
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun uploadImageToCloudinary(userId: String, localFilePath: String?): String? {
        if (localFilePath == null) return null
        
        val file = File(localFilePath)
        if (file.exists()) {
            val url = CloudinaryHelper.uploadImage(localFilePath, "USERS", userId)
            android.util.Log.d("UploadProfileWorker", "✅ Imagen subida con éxito (sobrescrita): $url")
            file.delete()
            return url
        }
        return null
    }

    private suspend fun updateFirestore(userId: String, name: String, finalPhotoUrl: String?) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "uploadStatus" to "Completado"
        )
        finalPhotoUrl?.let { updates["photoUrl"] = it }
        
        firestore.collection("users").document(userId)
            .set(updates, SetOptions.merge())
            .await()
    }

    private suspend fun syncLocalRoom(userId: String) {
        val finalSnapshot = firestore.collection("users").document(userId)
            .get(com.google.firebase.firestore.Source.SERVER).await()
        val data = finalSnapshot.data ?: return

        val resolvedName = (data["name"] as? String) ?: generateGenericName(userId)
        val profile = Profile(
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
        profileDao.insertProfile(profile.toEntity())
        android.util.Log.d("UploadProfileWorker", "✅ Room sincronizado localmente")
    }

    private fun generateGenericName(userId: String): String {
        val hash = userId.hashCode().let { if (it < 0) -it else it }
        val suffix = hash.toString().takeLast(4).padStart(4, '0')
        return "user$suffix"
    }
}
