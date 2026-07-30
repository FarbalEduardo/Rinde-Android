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

    override suspend fun doWork(): Result {
        android.util.Log.i(TAG, "▶️ doWork() iniciado para actualización — intento #${runAttemptCount + 1}")
        NotificationHelper.createNotificationChannels(context)

        try {
            setForeground(getForegroundInfo())
            android.util.Log.d(TAG, "✅ Foreground service activo")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "⚠️ setForeground() falló: ${e.message}")
        }

        val postId = inputData.getString("postId") ?: return Result.failure()
        val title = inputData.getString("title") ?: ""
        val descriptionLong = inputData.getString("descriptionLong") ?: ""
        val category = inputData.getString("category") ?: "Otros"
        val locationName = inputData.getString("locationName") ?: ""
        val localFilePaths = inputData.getStringArray("localFilePaths") ?: emptyArray()
        val oldPhotoUrls = inputData.getStringArray("oldPhotoUrls")?.toList() ?: emptyList()

        val websiteName = inputData.getString("websiteName")
        val productLink = inputData.getString("productLink")
        val storeName = inputData.getString("storeName")
        val offerType = if (!productLink.isNullOrBlank()) "ONLINE" else "PHYSICAL"

        val normalPrice = inputData.getDouble("normalPrice", Double.NaN).takeUnless { it.isNaN() }
        val discountPrice = inputData.getDouble("discountPrice", Double.NaN).takeUnless { it.isNaN() }
        val currency = inputData.getString("currency") ?: "MXN"
        val couponCode = inputData.getString("couponCode")
        val discountPercentage = inputData.getInt("discountPercentage", Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE }
        val isAvailable = inputData.getBoolean("isAvailable", true)
        val condition = inputData.getString("condition") ?: "Nuevo"

        return try {
            // 1. Obtener la publicación actual de Firestore para identificar fotos
            val postDoc = firestore.collection("posts").document(postId).get().await()
            val currentPhotos = postDoc.get("photos") as? List<*>
            val currentPhotoUrls = currentPhotos?.mapNotNull { it as? String } ?: emptyList()

            // 2. Identificar y eliminar las fotos antiguas descartadas por el usuario
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

            // 3. Subir fotos nuevas a Cloudinary
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
                    file.delete() // Cleanup
                }
            }

            // 4. Combinar fotos viejas conservadas + fotos nuevas subidas
            val finalPhotos = oldPhotoUrls + uploadedPhotoUrls

            // 5. Actualizar Firestore
            val updates = hashMapOf<String, Any?>(
                "title" to title,
                "descriptionLong" to descriptionLong,
                "descriptionShort" to if (descriptionLong.length > 50) descriptionLong.take(50) + "..." else descriptionLong,
                "photos" to finalPhotos,
                "category" to category,
                "location" to mapOf("name" to locationName),
                "offerType" to offerType,
                "websiteName" to websiteName,
                "productLink" to productLink,
                "storeName" to storeName,
                "normalPrice" to normalPrice,
                "discountPrice" to discountPrice,
                "currency" to currency,
                "couponCode" to couponCode,
                "discountPercentage" to discountPercentage,
                "isAvailable" to isAvailable,
                "condition" to condition
            )

            android.util.Log.d(TAG, "📤 Enviando actualización a Firestore para postId=$postId...")
            firestore.collection("posts").document(postId).update(updates).await()

            // 6. Sincronizar Room localmente
            val updatedDoc = firestore.collection("posts").document(postId).get().await()
            val updatedPostDto = updatedDoc.toObject(CommunityPostDto::class.java)?.copy(id = updatedDoc.id)
            if (updatedPostDto != null) {
                val domain = updatedPostDto.toDomain()
                postDao.upsertPosts(listOf(domain.toEntity()))
                android.util.Log.d(TAG, "💾 Post actualizado en Room local: $postId")
            }

            try { NotificationHelper.showSuccessNotification(context) } catch (_: Exception) {}
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "🔥 ERROR CRÍTICO actualizando post: ${e.javaClass.simpleName} — ${e.message}", e)
            try { NotificationHelper.showErrorNotification(context) } catch (_: Exception) {}
            if (runAttemptCount < 3) Result.retry() else Result.failure()
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
