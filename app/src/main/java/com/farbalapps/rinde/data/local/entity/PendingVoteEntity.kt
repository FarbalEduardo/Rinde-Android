package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity

@Entity(tableName = "pending_votes", primaryKeys = ["postId", "userId"])
data class PendingVoteEntity(
    val postId: String,
    val userId: String,
    val voteValue: Int,
    val authorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastFailureReason: String? = null
)
