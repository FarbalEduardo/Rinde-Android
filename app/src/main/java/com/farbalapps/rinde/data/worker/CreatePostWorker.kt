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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File

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
        val title = inputData.getString("title") ?: ""
        val descriptionLong = inputData.getString("descriptionLong") ?: ""
        val category = inputData.getString("category") ?: "Otros"
        val locationName = inputData.getString("locationName") ?: ""
        val localFilePaths = inputData.getStringArray("localFilePaths") ?: emptyArray()

        return try {
            val uploadedPhotoUrls = mutableListOf<String>()

            // 1. Upload Images to Cloudinary (REST API)
            localFilePaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    try {
                        val url = CloudinaryHelper.uploadImage(path, "PUBLICATIONS")
                        uploadedPhotoUrls.add(url)
                    } catch (e: Exception) {
                        android.util.Log.e("CreatePostWorker", "Error subiendo a Cloudinary: ${e.message}")
                    }
                    
                    // Cleanup local temp file
                    file.delete()
                }
            }

            // 2. Create Firestore Document
            val postRef = firestore.collection("posts").document()
            val post = CommunityPost(
                id = postRef.id,
                authorId = authorId,
                authorName = authorName,
                title = title,
                descriptionLong = descriptionLong,
                descriptionShort = if (descriptionLong.length > 50) descriptionLong.take(50) + "..." else descriptionLong,
                photos = uploadedPhotoUrls,
                category = category,
                location = PostLocation(name = locationName),
                isActive = true,
                score = 0f
            )

            postRef.set(post).await()

            // 3. Success Notification
            NotificationHelper.showSuccessNotification(context)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            NotificationHelper.showErrorNotification(context)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            1001,
            NotificationHelper.getPublishingNotification(context)
        )
    }
}
