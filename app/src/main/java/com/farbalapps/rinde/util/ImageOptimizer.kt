package com.farbalapps.rinde.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageOptimizer {

    /**
     * Optimiza una imagen desde un Uri, redimensionándola y convirtiéndola a WebP.
     * Retorna el File resultante en el directorio de caché.
     */
    fun optimizeImage(context: Context, uri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024): File? {
        return try {
            // Detectar rotación original del archivo usando ExifInterface
            val orientation = getOrientation(context, uri)

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Calcular escala manteniedo relación de aspecto
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = Math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height).coerceAtMost(1.0f)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            // Aplicar rotación si es necesario
            if (orientation != 0f) {
                matrix.postRotate(orientation)
            }

            val resizedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, width, height, matrix, true
            )

            // Crear archivo temporal
            val outputFile = File(context.cacheDir, "optimized_profile_${System.currentTimeMillis()}.webp")
            val out = FileOutputStream(outputFile)

            // Comprimir a WebP (formato recomendado por Google)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resizedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
            } else {
                @Suppress("DEPRECATION")
                resizedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
            }

            out.flush()
            out.close()

            // Liberar memoria
            if (resizedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getOrientation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }
}
