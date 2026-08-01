package com.farbalapps.rinde.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.util.CloudinaryHelper
import com.farbalapps.rinde.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File

@HiltWorker
class UpdatePostWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UpdatePostWorker"
    }

    data class UpdatePostData(
        val postId: String,
        val title: String,
        val descriptionLong: String,
        val category: String,
        val locationName: String,
        val localFilePaths: Array<String>,
        val oldPhotoUrls: List<String>,
        val websiteName: String?,
        val productLink: String?,
        val storeName: String?,
        val offerType: String,
        val normalPrice: Double?,
        val discountPrice: Double?,
        val currency: String,
        val couponCode: String?,
        val discountPercentage: Int?,
        val isAvailable: Boolean,
        val condition: String
    )

    override suspend fun doWork(): Result {
        android.util.Log.i(TAG, "▶️ doWork() iniciado para actualización — intento #${runAttemptCount + 1}")
        NotificationHelper.createNotificationChannels(context)

        try {
            setForeground(getForegroundInfo())
            android.util.Log.d(TAG, "✅ Foreground service activo")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "⚠️ setForeground() falló: ${e.message}")
        }

        val data = extractUpdateData() ?: return Result.failure()

        return try {
            deleteDiscardedPhotos(data.postId, data.oldPhotoUrls)
            
            val uploadedPhotoUrls = uploadNewPhotos(data.localFilePaths)
            val finalPhotos = data.oldPhotoUrls + uploadedPhotoUrls

            updateFirestoreAndRoom(data, finalPhotos)

            try { NotificationHelper.showSuccessNotification(context) } catch (_: Exception) {}
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "🔥 ERROR CRÍTICO actualizando post: ${e.javaClass.simpleName} — ${e.message}", e)
            try { NotificationHelper.showErrorNotification(context) } catch (_: Exception) {}
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun extractUpdateData(): UpdatePostData? {
        val postId = inputData.getString("postId") ?: return null
        val productLink = inputData.getString("productLink")
        
        return UpdatePostData(
            postId = postId,
            title = inputData.getString("title") ?: "",
            descriptionLong = inputData.getString("descriptionLong") ?: "",
            category = inputData.getString("category") ?: "Otros",
            locationName = inputData.getString("locationName") ?: "",
            localFilePaths = inputData.getStringArray("localFilePaths") ?: emptyArray(),
            oldPhotoUrls = inputData.getStringArray("oldPhotoUrls")?.toList() ?: emptyList(),
            websiteName = inputData.getString("websiteName"),
            productLink = productLink,
            storeName = inputData.getString("storeName"),
            offerType = if (!productLink.isNullOrBlank()) "ONLINE" else "PHYSICAL",
            normalPrice = inputData.getDouble("normalPrice", Double.NaN).takeUnless { it.isNaN() },
            discountPrice = inputData.getDouble("discountPrice", Double.NaN).takeUnless { it.isNaN() },
            currency = inputData.getString("currency") ?: "MXN",
            couponCode = inputData.getString("couponCode"),
            discountPercentage = inputData.getInt("discountPercentage", Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE },
            isAvailable = inputData.getBoolean("isAvailable", true),
            condition = inputData.getString("condition") ?: "Nuevo"
        )
    }

    private suspend fun deleteDiscardedPhotos(postId: String, oldPhotoUrls: List<String>) {
        val postDoc = firestore.collection("posts").document(postId).get().await()
        val currentPhotos = postDoc.get("photos") as? List<*>
        val currentPhotoUrls = currentPhotos?.mapNotNull { it as? String } ?: emptyList()

        val oldPhotoSet = oldPhotoUrls.toSet()
        val deletedPhotoUrls = currentPhotoUrls.filter { it !in oldPhotoSet }
        if (deletedPhotoUrls.isNotEmpty()) {
            android.util.Log.d(TAG, "🗑️ Se detectaron ${deletedPhotoUrls.size} fotos descartadas para eliminar de Cloudinary")
            deletedPhotoUrls.forEach { photoUrl ->
                try {
                    CloudinaryHelper.deleteImage(photoUrl)
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "⚠️ No se pudo eliminar la imagen $photoUrl de Cloudinary: ${e.message}")
                }
            }
        }
    }

    private suspend fun uploadNewPhotos(localFilePaths: Array<String>): List<String> {
        val uploadedPhotoUrls = mutableListOf<String>()
        localFilePaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    android.util.Log.d(TAG, "☁️ Subiendo imagen nueva: $path")
                    val url = CloudinaryHelper.uploadImage(path, "Post")
                    uploadedPhotoUrls.add(url)
                    android.util.Log.d(TAG, "✅ Imagen subida: $url")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "❌ Error subiendo a Cloudinary: ${e.message}", e)
                    throw e
                }
                file.delete()
            }
        }
        return uploadedPhotoUrls
    }

    private suspend fun updateFirestoreAndRoom(data: UpdatePostData, finalPhotos: List<String>) {
        val updates = hashMapOf<String, Any?>(
            "title" to data.title,
            "descriptionLong" to data.descriptionLong,
            "descriptionShort" to if (data.descriptionLong.length > 50) data.descriptionLong.take(50) + "..." else data.descriptionLong,
            "photos" to finalPhotos,
            "category" to data.category,
            "location" to mapOf("name" to data.locationName),
            "offerType" to data.offerType,
            "websiteName" to data.websiteName,
            "productLink" to data.productLink,
            "storeName" to data.storeName,
            "normalPrice" to data.normalPrice,
            "discountPrice" to data.discountPrice,
            "currency" to data.currency,
            "couponCode" to data.couponCode,
            "discountPercentage" to data.discountPercentage,
            "isAvailable" to data.isAvailable,
            "condition" to data.condition
        )

        android.util.Log.d(TAG, "📤 Enviando actualización a Firestore para postId=${data.postId}...")
        firestore.collection("posts").document(data.postId).update(updates).await()

        val updatedDoc = firestore.collection("posts").document(data.postId).get().await()
        val updatedPostDto = updatedDoc.toObject(CommunityPostDto::class.java)?.copy(id = updatedDoc.id)
        if (updatedPostDto != null) {
            val domain = updatedPostDto.toDomain()
            postDao.upsertPosts(listOf(domain.toEntity()))
            android.util.Log.d(TAG, "💾 Post actualizado en Room local: ${data.postId}")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1002,
                NotificationHelper.getPublishingNotification(context),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                1002,
                NotificationHelper.getPublishingNotification(context)
            )
        }
    }
}
