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

    override suspend fun doWork(): Result {
        android.util.Log.i(TAG, "▶️ doWork() iniciado — intento #${runAttemptCount + 1}")
        NotificationHelper.createNotificationChannels(context)

        // setForeground es opcional: puede fallar en Android 14 si la app ya pasó al fondo.
        // Lo intentamos pero continuamos aunque no pueda iniciar como foreground service.
        try {
            setForeground(getForegroundInfo())
            android.util.Log.d(TAG, "✅ Foreground service activo")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "⚠️ setForeground() falló (posiblemente sin permiso POST_NOTIFICATIONS o app en segundo plano): ${e.message}")
            // Continuamos sin modo foreground — el Worker seguirá ejecutándose
        }

        val authorId = inputData.getString("authorId") ?: run {
            android.util.Log.e(TAG, "❌ authorId es null — abortando")
            return Result.failure()
        }
        val authorName = inputData.getString("authorName") ?: "Usuario"
        val authorPhotoUrl = inputData.getString("authorPhotoUrl")
        val title = inputData.getString("title") ?: ""
        val descriptionLong = inputData.getString("descriptionLong") ?: ""
        val category = inputData.getString("category") ?: "Otros"
        val locationName = inputData.getString("locationName") ?: ""
        val localFilePaths = inputData.getStringArray("localFilePaths") ?: emptyArray()

        android.util.Log.d(TAG, "📋 Datos recibidos: title='$title', fotos=${localFilePaths.size}, authorId=$authorId")

        // New v3 fields
        val offerType = inputData.getString("offerType") ?: "UNSPECIFIED"
        val websiteName = inputData.getString("websiteName")
        val productLink = inputData.getString("productLink")
        val storeName = inputData.getString("storeName")
        val userReputationScore = inputData.getFloat("userReputationScore", 0f)

        // Pricing/details fields
        val normalPrice = inputData.getDouble("normalPrice", Double.NaN).takeUnless { it.isNaN() }
        val discountPrice = inputData.getDouble("discountPrice", Double.NaN).takeUnless { it.isNaN() }
        val currency = inputData.getString("currency") ?: "MXN"
        val couponCode = inputData.getString("couponCode")
        val discountPercentage = inputData.getInt("discountPercentage", Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE }
        val isAvailable = inputData.getBoolean("isAvailable", true)
        val condition = inputData.getString("condition") ?: "Nuevo"

        android.util.Log.d(TAG, "💰 Precio normal=$normalPrice, descuento=$discountPrice, moneda=$currency")

        return try {
            android.util.Log.d(TAG, "🛠️ Iniciando subida para: $title con ${localFilePaths.size} imágenes")
            val uploadedPhotoUrls = mutableListOf<String>()

            // 1. Upload Images to Cloudinary (REST API)
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
                        throw e // Forzar reintento del Worker
                    }
                    // Cleanup local temp file
                    file.delete()
                } else {
                    android.util.Log.w(TAG, "⚠️ Archivo no encontrado: $path — se omite")
                }
            }

            if (uploadedPhotoUrls.isEmpty() && localFilePaths.isNotEmpty()) {
                android.util.Log.e(TAG, "❌ Ninguna imagen se subió correctamente")
                return if (runAttemptCount < 3) Result.retry() else Result.failure()
            }

            // 2. Create Firestore Document
            android.util.Log.d(TAG, "📝 Creando documento en Firestore...")
            val postRef = firestore.collection("posts").document()

            val postMap = hashMapOf<String, Any?>(
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
                "votesScore" to 0,
                "verificationStatus" to "PENDING",
                "reportCount" to 0,
                "userReputationScore" to userReputationScore,
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
                "condition" to condition,
                "isRecommended" to false,
                "score" to 0f
            )

            android.util.Log.d(TAG, "📤 Enviando batch a Firestore (postId=${postRef.id})...")
            firestore.runBatch { batch ->
                batch.set(postRef, postMap)
                val userRef = firestore.collection("users").document(authorId)
                batch.set(userRef, mapOf("postsCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
            }.await()
            android.util.Log.i(TAG, "✨ Post publicado exitosamente en Firestore: ${postRef.id}")

            // 2.5 Actualización Optimista en Room
            val entity = CommunityPostEntity(
                id = postRef.id,
                authorId = authorId,
                authorName = authorName,
                authorPhotoUrl = authorPhotoUrl,
                timestamp = System.currentTimeMillis(),
                title = title,
                descriptionLong = descriptionLong,
                descriptionShort = if (descriptionLong.length > 50) descriptionLong.take(50) + "..." else descriptionLong,
                photos = uploadedPhotoUrls,
                category = category,
                locationName = locationName,
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
                userReputationScore = userReputationScore,
                isAuthorVerified = false,
                offerType = offerType,
                websiteName = websiteName,
                productLink = productLink,
                storeName = storeName,
                isRecommended = false,
                expiresAt = null,
                normalPrice = normalPrice,
                discountPrice = discountPrice,
                currency = currency,
                couponCode = couponCode,
                discountPercentage = discountPercentage,
                isAvailable = isAvailable,
                condition = condition,
                myVoteValue = 0,
                isSavedByMe = false,
                authorTrustScore = 0f,
                authorTrustLevel = "NEW"
            )
            postDao.upsertPosts(listOf(entity))
            android.util.Log.d(TAG, "💾 Post guardado en Room local: ${postRef.id}")

            // 3. Success Notification
            try { NotificationHelper.showSuccessNotification(context) } catch (_: Exception) {}
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "🔥 ERROR CRÍTICO publicando post: ${e.javaClass.simpleName} — ${e.message}", e)
            try { NotificationHelper.showErrorNotification(context) } catch (_: Exception) {}
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
