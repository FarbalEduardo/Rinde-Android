package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity

@Entity(tableName = "user_votes", primaryKeys = ["postId", "userId"])
data class UserVoteEntity(
    val postId: String,
    val userId: String,
    val voteValue: Int,   // 1 = Real, -1 = Falso
    val confirmedAt: Long = System.currentTimeMillis()
)
