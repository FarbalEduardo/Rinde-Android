package com.farbalapps.rinde.domain.model

/**
 * Represents a comment on a community post.
 * Stored in Firebase Realtime Database under /comments/{postId}/{commentId}.
 * Images are uploaded to Cloudinary under the "Comments" folder.
 */
data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,       // Optional image (Cloudinary "Comments" folder)
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val repliesCount: Int = 0
)
