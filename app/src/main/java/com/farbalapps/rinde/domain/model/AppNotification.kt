package com.farbalapps.rinde.domain.model

enum class NotificationType {
    POST_EXPIRED,
    NEW_COMMENT,
    POST_VERIFIED
}

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.NEW_COMMENT,
    val postId: String = "",
    val postTitle: String = "",
    val actorName: String? = null,
    val actorPhotoUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
