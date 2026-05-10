package com.farbalapps.rinde.util

import com.farbalapps.rinde.BuildConfig

object Config {
    
    // CLOUDINARY CONFIG — Valores inyectados desde local.properties (BuildConfig)
    const val CLOUDINARY_CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    const val CLOUDINARY_API_KEY = BuildConfig.CLOUDINARY_API_KEY
    const val CLOUDINARY_API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
}
