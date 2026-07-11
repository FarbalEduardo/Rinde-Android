package com.farbalapps.rinde.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * CloudinaryHelper — Subida de imágenes a Cloudinary usando Signed Uploads vía REST API.
 *
 * ¿Por qué Signed y no Unsigned?
 * - Signed: La firma se genera con tu API Secret, demostrando que la petición es legítima.
 *   Cloudinary confía plenamente en la subida. No necesitas configurar presets "Unsigned".
 * - Unsigned: Depende de un Upload Preset configurado como "Unsigned" en la consola.
 *   Si el preset no está bien configurado, falla con el error "must be whitelisted".
 *
 * Para una app con pocos usuarios (como Rinde), Signed desde el cliente es práctico y funcional.
 * Para producción a gran escala, la firma debería generarse en un backend (Cloud Functions).
 */
object CloudinaryHelper {

    private val client = OkHttpClient()

    /**
     * Sube una imagen a Cloudinary usando Unsigned Upload (API REST directa).
     *
     * @param filePath Ruta absoluta del archivo local a subir.
     * @param folder Carpeta destino en Cloudinary (ej: "USERS", "PUBLICATIONS").
     * @return La URL segura (HTTPS) de la imagen subida.
     * @throws Exception Si la subida falla.
     */
    suspend fun uploadImage(filePath: String, folder: String, publicId: String? = null): String = withContext(Dispatchers.IO) {
        android.util.Log.d("CloudinaryHelper", "📤 Iniciando subida UNSIGNED: $filePath → carpeta: $folder, publicId: $publicId")

        val file = File(filePath)
        if (!file.exists()) {
            throw Exception("Cloudinary Error: El archivo no existe: $filePath")
        }

        // Construir la petición multipart para UNSIGNED upload
        val requestBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", "rinde_unsigned_preset")
            .addFormDataPart("folder", folder)

        // Si hay un publicId fijo, se envía para sobrescribir (requiere preset con overwrite activo)
        if (!publicId.isNullOrEmpty()) {
            requestBuilder.addFormDataPart("public_id", publicId)
        }

        val requestBody = requestBuilder.build()
        val url = "https://api.cloudinary.com/v1_1/${Config.CLOUDINARY_CLOUD_NAME}/image/upload"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        android.util.Log.d("CloudinaryHelper", "🌐 Enviando a: $url")

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            android.util.Log.e("CloudinaryHelper", "❌ ERROR HTTP ${response.code}: $responseBody")
            throw Exception("Cloudinary Error HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val secureUrl = json.optString("secure_url", "")

        if (secureUrl.isBlank()) {
            android.util.Log.e("CloudinaryHelper", "❌ No se encontró secure_url en la respuesta: $responseBody")
            throw Exception("Cloudinary: No URL in response")
        }

        android.util.Log.i("CloudinaryHelper", "✅ ÉXITO: $secureUrl")
        secureUrl
    }

    /**
     * @deprecated Omitido por seguridad. Requería CLOUDINARY_API_SECRET expuesta en el APK.
     * En el plan Firebase Free/Spark, las imágenes quedan huérfanas en Cloudinary al eliminarse o editarse posts.
     */
    @Deprecated("Omitido para no exponer el API_SECRET en la app")
    suspend fun deleteImage(cloudinaryUrl: String): Boolean {
        android.util.Log.w("CloudinaryHelper", "deleteImage llamado pero está desactivado por seguridad de API_SECRET")
        return false
    }

    /**
     * Extrae el public_id de una URL de Cloudinary.
     *
     * Ejemplo:
     *   URL: https://res.cloudinary.com/dnl3qdnpc/image/upload/v1234567890/USERS/abc123.jpg
     *   public_id: USERS/abc123
     */
    fun extractPublicId(url: String): String? {
        if (!url.contains("cloudinary.com")) return null

        return try {
            // Buscar "/upload/" o "/image/upload/" y tomar todo lo que viene después
            val uploadIndex = url.indexOf("/upload/")
            if (uploadIndex == -1) return null

            val afterUpload = url.substring(uploadIndex + "/upload/".length)

            // Saltar el versionado (v1234567890/) si existe
            val pathWithoutVersion = if (afterUpload.matches(Regex("^v\\d+/.*"))) {
                afterUpload.substringAfter("/")
            } else {
                afterUpload
            }

            // Quitar la extensión del archivo (.jpg, .png, .webp, etc.)
            pathWithoutVersion.substringBeforeLast(".")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Genera un hash SHA-1 para la firma de Cloudinary.
     */
    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
