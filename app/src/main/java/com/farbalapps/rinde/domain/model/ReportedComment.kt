package com.farbalapps.rinde.domain.model

/**
 * Represents a comment or reply that has been reported by a user.
 * Stored in Firestore under the "reported_comments" collection.
 */
data class ReportedComment(
    val reportId: String = "",
    val postId: String = "",
    val commentId: String = "",
    val commentText: String = "",
    val authorId: String = "",       // User who wrote the reported comment/reply
    val reporterId: String = "",     // User who reported the comment/reply
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"   // "PENDING", "RESOLVED", etc.
)
