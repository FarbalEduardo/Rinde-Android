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
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.entity.CommunityPostEntity

@HiltWorker
class CreatePostWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CreatePostWorker"
    }

    data class PostWorkerData(
        val authorId: String,
        val authorName: String,
        val authorPhotoUrl: String?,
        val title: String,
        val descriptionLong: String,
        val category: String,
        val locationName: String,
        val localFilePaths: Array<String>,
        val offerType: String,
        val websiteName: String?,
        val productLink: String?,
        val storeName: String?,
        val userReputationScore: Float,
        val normalPrice: Double?,
        val discountPrice: Double?,
        val currency: String,
        val couponCode: String?,
        val discountPercentage: Int?,
        val isAvailable: Boolean,
        val condition: String
    )

    override suspend fun doWork(): Result {
        android.util.Log.i(TAG, "▶️ doWork() iniciado — intento #${runAttemptCount + 1}")
        NotificationHelper.createNotificationChannels(context)

        try {
            setForeground(getForegroundInfo())
            android.util.Log.d(TAG, "✅ Foreground service activo")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "⚠️ setForeground() falló (posiblemente sin permiso POST_NOTIFICATIONS o app en segundo plano): ${e.message}")
        }

        val data = extractPostData() ?: run {
            android.util.Log.e(TAG, "❌ authorId es null — abortando")
            return Result.failure()
        }

        android.util.Log.d(TAG, "📋 Datos recibidos: title='${data.title}', fotos=${data.localFilePaths.size}, authorId=${data.authorId}")
        android.util.Log.d(TAG, "💰 Precio normal=${data.normalPrice}, descuento=${data.discountPrice}, moneda=${data.currency}")

        return try {
            android.util.Log.d(TAG, "🛠️ Iniciando subida para: ${data.title} con ${data.localFilePaths.size} imágenes")
            val uploadedPhotoUrls = uploadImages(data.localFilePaths)

            if (uploadedPhotoUrls.isEmpty() && data.localFilePaths.isNotEmpty()) {
                android.util.Log.e(TAG, "❌ Ninguna imagen se subió correctamente")
                return if (runAttemptCount < 3) Result.retry() else Result.failure()
            }

            android.util.Log.d(TAG, "📝 Creando documento en Firestore...")
            val postId = saveToFirestore(data, uploadedPhotoUrls)
            android.util.Log.i(TAG, "✨ Post publicado exitosamente en Firestore: $postId")

            saveToLocalRoom(postId, data, uploadedPhotoUrls)
            android.util.Log.d(TAG, "💾 Post guardado en Room local: $postId")

            try { NotificationHelper.showSuccessNotification(context) } catch (_: Exception) {}
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "🔥 ERROR CRÍTICO publicando post: ${e.javaClass.simpleName} — ${e.message}", e)
            try { NotificationHelper.showErrorNotification(context) } catch (_: Exception) {}
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun extractPostData(): PostWorkerData? {
        val authorId = inputData.getString("authorId") ?: return null
        return PostWorkerData(
            authorId = authorId,
            authorName = inputData.getString("authorName") ?: "Usuario",
            authorPhotoUrl = inputData.getString("authorPhotoUrl"),
            title = inputData.getString("title") ?: "",
            descriptionLong = inputData.getString("descriptionLong") ?: "",
            category = inputData.getString("category") ?: "Otros",
            locationName = inputData.getString("locationName") ?: "",
            localFilePaths = inputData.getStringArray("localFilePaths") ?: emptyArray(),
            offerType = inputData.getString("offerType") ?: "UNSPECIFIED",
            websiteName = inputData.getString("websiteName"),
            productLink = inputData.getString("productLink"),
            storeName = inputData.getString("storeName"),
            userReputationScore = inputData.getFloat("userReputationScore", 0f),
            normalPrice = inputData.getDouble("normalPrice", Double.NaN).takeUnless { it.isNaN() },
            discountPrice = inputData.getDouble("discountPrice", Double.NaN).takeUnless { it.isNaN() },
            currency = inputData.getString("currency") ?: "MXN",
            couponCode = inputData.getString("couponCode"),
            discountPercentage = inputData.getInt("discountPercentage", Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE },
            isAvailable = inputData.getBoolean("isAvailable", true),
            condition = inputData.getString("condition") ?: "Nuevo"
        )
    }

    private suspend fun uploadImages(localFilePaths: Array<String>): List<String> {
        val uploadedPhotoUrls = mutableListOf<String>()
        localFilePaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    android.util.Log.d(TAG, "☁️ Subiendo imagen: $path")
                    val url = CloudinaryHelper.uploadImage(path, "Post")
                    uploadedPhotoUrls.add(url)
                    android.util.Log.d(TAG, "✅ Imagen subida: $url")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "❌ Error subiendo a Cloudinary: ${e.message}", e)
                    throw e
                }
                file.delete()
            } else {
                android.util.Log.w(TAG, "⚠️ Archivo no encontrado: $path — se omite")
            }
        }
        return uploadedPhotoUrls
    }

    private suspend fun saveToFirestore(data: PostWorkerData, uploadedPhotoUrls: List<String>): String {
        val postRef = firestore.collection("posts").document()
        val postMap = hashMapOf<String, Any?>(
            "id" to postRef.id,
            "authorId" to data.authorId,
            "authorName" to data.authorName,
            "authorPhotoUrl" to data.authorPhotoUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            "title" to data.title,
            "descriptionLong" to data.descriptionLong,
            "descriptionShort" to if (data.descriptionLong.length > 50) data.descriptionLong.take(50) + "..." else data.descriptionLong,
            "photos" to uploadedPhotoUrls,
            "category" to data.category,
            "location" to mapOf("name" to data.locationName),
            "isActive" to true,
            "likesCount" to 0,
            "commentsCount" to 0,
            "truthCount" to 0,
            "falseCount" to 0,
            "votesScore" to 0,
            "verificationStatus" to "PENDING",
            "reportCount" to 0,
            "userReputationScore" to data.userReputationScore,
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
            "condition" to data.condition,
            "isRecommended" to false,
            "score" to 0f
        )

        firestore.runBatch { batch ->
            batch.set(postRef, postMap)
            val userRef = firestore.collection("users").document(data.authorId)
            batch.set(userRef, mapOf("postsCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
        }.await()
        return postRef.id
    }

    private suspend fun saveToLocalRoom(postId: String, data: PostWorkerData, uploadedPhotoUrls: List<String>) {
        val entity = CommunityPostEntity(
            id = postId,
            authorId = data.authorId,
            authorName = data.authorName,
            authorPhotoUrl = data.authorPhotoUrl,
            timestamp = System.currentTimeMillis(),
            title = data.title,
            descriptionLong = data.descriptionLong,
            descriptionShort = if (data.descriptionLong.length > 50) data.descriptionLong.take(50) + "..." else data.descriptionLong,
            photos = uploadedPhotoUrls,
            category = data.category,
            locationName = data.locationName,
            latitude = null,
            longitude = null,
            isActive = true,
            likesCount = 0,
            commentsCount = 0,
            truthCount = 0,
            falseCount = 0,
            votesScore = 0,
            verificationStatus = "PENDING",
            reportCount = 0,
            userReputationScore = data.userReputationScore,
            isAuthorVerified = false,
            offerType = data.offerType,
            websiteName = data.websiteName,
            productLink = data.productLink,
            storeName = data.storeName,
            isRecommended = false,
            expiresAt = null,
            normalPrice = data.normalPrice,
            discountPrice = data.discountPrice,
            currency = data.currency,
            couponCode = data.couponCode,
            discountPercentage = data.discountPercentage,
            isAvailable = data.isAvailable,
            condition = data.condition,
            myVoteValue = 0,
            isSavedByMe = false,
            authorTrustScore = 0f,
            authorTrustLevel = "NEW"
        )
        postDao.upsertPosts(listOf(entity))
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
