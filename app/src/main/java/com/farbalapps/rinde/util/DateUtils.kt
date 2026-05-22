package com.farbalapps.rinde.util

object DateUtils {
    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Ahora"
            diff < 3600_000 -> "Hace ${diff / 60_000} min"
            diff < 86400_000 -> "Hace ${diff / 3600_000} h"
            else -> "Hace ${diff / 86400_000} d"
        }
    }
}
