package com.farbalapps.rinde.domain.model

/**
 * Represents a reply to a comment on a community post.
 * Stored in Firebase Realtime Database under /replies/{commentId}/{replyId}.
 * Images are uploaded to Cloudinary under the "Comments" folder.
 */
data class Reply(
    val id: String = "",
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,       // Optional image (Cloudinary "Comments" folder)
    val mentionedUser: String? = null,  // For @mentions display
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0
)
