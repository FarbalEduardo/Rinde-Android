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

            val resizedBitmap = processBitmap(originalBitmap, maxWidth, maxHeight, orientation)
            val outputFile = saveBitmapToFile(context, resizedBitmap)

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

    private fun processBitmap(
        originalBitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        orientation: Float
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = Math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height).coerceAtMost(1.0f)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        if (orientation != 0f) {
            matrix.postRotate(orientation)
        }

        return Bitmap.createBitmap(
            originalBitmap, 0, 0, width, height, matrix, true
        )
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
        val outputFile = File(context.cacheDir, "optimized_profile_${System.currentTimeMillis()}.webp")
        val out = FileOutputStream(outputFile)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
        } else {
            @Suppress("DEPRECATION")
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }

        out.flush()
        out.close()
        return outputFile
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
