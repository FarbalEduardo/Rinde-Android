package com.farbalapps.rinde.domain.model

data class PostLocation(
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class CommunityPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val descriptionShort: String = "",
    val descriptionLong: String = "",
    val photos: List<String> = emptyList(),
    val category: String = "Otros",
    val location: PostLocation = PostLocation(),
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val isRecommended: Boolean = false,
    val votes: Int = 0,
    val score: Float = 0f,
    val expiresAt: Long? = null,
    val isActive: Boolean = true
)
