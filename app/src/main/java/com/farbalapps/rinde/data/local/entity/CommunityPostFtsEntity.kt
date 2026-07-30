package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = CommunityPostEntity::class)
@Entity(tableName = "community_posts_fts")
data class CommunityPostFtsEntity(
    val title: String,
    val descriptionShort: String,
    val storeName: String?,
    val category: String,
    val authorName: String,
    val couponCode: String?
)
