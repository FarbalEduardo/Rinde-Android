package com.farbalapps.rinde.util

import com.farbalapps.rinde.BuildConfig

object CloudinaryUrlBuilder {
    private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME

    /**
     * Miniatura del feed: 400px de ancho, calidad automática, formato automático (WebP en Android).
     */
    fun feedThumbnail(originalUrl: String): String = buildUrl(
        url = originalUrl,
        width = 400,
        quality = "auto",
        format = "auto"
    )

    /**
     * Detalle del post: 800px de ancho, calidad alta, formato automático.
     */
    fun postDetail(originalUrl: String): String = buildUrl(
        url = originalUrl,
        width = 800,
        quality = "auto:best",
        format = "auto"
    )

    private fun buildUrl(url: String, width: Int, quality: String, format: String): String {
        if (!url.contains("cloudinary.com")) return url
        val uploadIndex = url.indexOf("/upload/")
        if (uploadIndex == -1) return url
        val base = url.substring(0, uploadIndex + "/upload/".length)
        val path = url.substring(uploadIndex + "/upload/".length)
        return "${base}w_$width,q_$quality,f_$format/$path"
    }
}
