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
     * Sube una imagen a Cloudinary usando Signed Upload (API REST directa).
     *
     * @param filePath Ruta absoluta del archivo local a subir.
     * @param folder Carpeta destino en Cloudinary (ej: "USERS", "PUBLICATIONS").
     * @return La URL segura (HTTPS) de la imagen subida.
     * @throws Exception Si la subida falla.
     */
    suspend fun uploadImage(filePath: String, folder: String): String = withContext(Dispatchers.IO) {
        android.util.Log.d("CloudinaryHelper", "📤 Iniciando subida SIGNED: $filePath → carpeta: $folder")

        val file = File(filePath)
        if (!file.exists()) {
            throw Exception("Cloudinary Error: El archivo no existe: $filePath")
        }

        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // Generar firma (signature) para Signed Upload
        // La firma se calcula como: SHA1("folder=X&timestamp=T" + API_SECRET)
        val paramsToSign = "folder=$folder&timestamp=$timestamp"
        val signature = sha1("$paramsToSign${Config.CLOUDINARY_API_SECRET}")

        android.util.Log.d("CloudinaryHelper", "🔑 Firma generada para timestamp=$timestamp")

        // Construir la petición multipart
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("api_key", Config.CLOUDINARY_API_KEY)
            .addFormDataPart("timestamp", timestamp)
            .addFormDataPart("signature", signature)
            .addFormDataPart("folder", folder)
            .build()

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
     * Elimina una imagen de Cloudinary usando Signed Destroy (API REST).
     *
     * @param cloudinaryUrl La URL completa de la imagen en Cloudinary.
     * @return true si se eliminó correctamente, false si falló.
     */
    suspend fun deleteImage(cloudinaryUrl: String): Boolean = withContext(Dispatchers.IO) {
        val publicId = extractPublicId(cloudinaryUrl)
        if (publicId == null) {
            android.util.Log.w("CloudinaryHelper", "⚠️ No se pudo extraer public_id de: $cloudinaryUrl")
            return@withContext false
        }

        android.util.Log.d("CloudinaryHelper", "🗑️ Eliminando imagen: $publicId")

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val paramsToSign = "public_id=$publicId&timestamp=$timestamp"
        val signature = sha1("$paramsToSign${Config.CLOUDINARY_API_SECRET}")

        val requestBody = okhttp3.FormBody.Builder()
            .add("public_id", publicId)
            .add("api_key", Config.CLOUDINARY_API_KEY)
            .add("timestamp", timestamp)
            .add("signature", signature)
            .build()

        val url = "https://api.cloudinary.com/v1_1/${Config.CLOUDINARY_CLOUD_NAME}/image/destroy"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val result = json.optString("result", "")

            if (result == "ok") {
                android.util.Log.i("CloudinaryHelper", "✅ Imagen eliminada: $publicId")
                true
            } else {
                android.util.Log.w("CloudinaryHelper", "⚠️ Resultado inesperado al eliminar: $responseBody")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryHelper", "❌ Error al eliminar imagen: ${e.message}")
            false
        }
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
