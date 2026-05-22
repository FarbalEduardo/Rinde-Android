package com.farbalapps.rinde.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.farbalapps.rinde.util.CloudinaryHelper
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File
import android.content.pm.ServiceInfo
import android.os.Build

@HiltWorker
class CreatePostWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannels(context)
        setForeground(getForegroundInfo())

        val authorId = inputData.getString("authorId") ?: return Result.failure()
        val authorName = inputData.getString("authorName") ?: "Usuario"
        val authorPhotoUrl = inputData.getString("authorPhotoUrl")
        val title = inputData.getString("title") ?: ""
        val descriptionLong = inputData.getString("descriptionLong") ?: ""
        val category = inputData.getString("category") ?: "Otros"
        val locationName = inputData.getString("locationName") ?: ""
        val localFilePaths = inputData.getStringArray("localFilePaths") ?: emptyArray()
        
        // New v3 fields
        val offerType = inputData.getString("offerType") ?: "UNSPECIFIED"
        val websiteName = inputData.getString("websiteName")
        val productLink = inputData.getString("productLink")
        val storeName = inputData.getString("storeName")
        val userReputationScore = inputData.getFloat("userReputationScore", 0f)

        return try {
            android.util.Log.d("CreatePostWorker", "🛠️ Iniciando subida para: $title con ${localFilePaths.size} imágenes")
            val uploadedPhotoUrls = mutableListOf<String>()

            // 1. Upload Images to Cloudinary (REST API)
            localFilePaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    try {
                        android.util.Log.d("CreatePostWorker", "☁️ Subiendo imagen: $path")
                        val url = CloudinaryHelper.uploadImage(path, "Post")
                        uploadedPhotoUrls.add(url)
                        android.util.Log.d("CreatePostWorker", "✅ Imagen subida: $url")
                    } catch (e: Exception) {
                        android.util.Log.e("CreatePostWorker", "❌ Error subiendo a Cloudinary: ${e.message}")
                        throw e // Forzar reintento del Worker
                    }
                    
                    // Cleanup local temp file
                    file.delete()
                } else {
                    android.util.Log.w("CreatePostWorker", "⚠️ Archivo no encontrado: $path")
                }
            }

            // 2. Create Firestore Document
            android.util.Log.d("CreatePostWorker", "📝 Creando documento en Firestore...")
            val postRef = firestore.collection("posts").document()
            
            // Build map to include Server Timestamp and ensure all counters are initialized
            val postMap = hashMapOf(
                "id" to postRef.id,
                "authorId" to authorId,
                "authorName" to authorName,
                "authorPhotoUrl" to authorPhotoUrl,
                "timestamp" to FieldValue.serverTimestamp(),
                "title" to title,
                "descriptionLong" to descriptionLong,
                "descriptionShort" to if (descriptionLong.length > 50) descriptionLong.take(50) + "..." else descriptionLong,
                "photos" to uploadedPhotoUrls,
                "category" to category,
                "location" to mapOf("name" to locationName),
                "isActive" to true,
                "likesCount" to 0,
                "commentsCount" to 0,
                "truthCount" to 0,
                "falseCount" to 0,
                
                // v3 Voting & Status
                "votesScore" to 0,
                "verificationStatus" to "PENDING",
                "reportCount" to 0,
                "userReputationScore" to userReputationScore,
                
                // Offer Details
                "offerType" to offerType,
                "websiteName" to websiteName,
                "productLink" to productLink,
                "storeName" to storeName,
                
                "isRecommended" to false,
                "score" to 0f
            )


            // Execute post creation and user stats update in a batch/transaction for atomic consistency
            firestore.runBatch { batch ->
                batch.set(postRef, postMap)
                
                // Increment postsCount in user document (using set with merge for resilience)
                val userRef = firestore.collection("users").document(authorId)
                batch.set(userRef, mapOf("postsCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
            }.await()
            android.util.Log.i("CreatePostWorker", "✨ Post publicado exitosamente en Firestore: ${postRef.id}")

            // 3. Success Notification
            NotificationHelper.showSuccessNotification(context)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("CreatePostWorker", "🔥 ERROR CRÍTICO publicando post: ${e.message}")
            e.printStackTrace()
            NotificationHelper.showErrorNotification(context)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1001,
                NotificationHelper.getPublishingNotification(context),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                1001,
                NotificationHelper.getPublishingNotification(context)
            )
        }
    }
}
