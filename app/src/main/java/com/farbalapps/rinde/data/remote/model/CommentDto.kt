package com.farbalapps.rinde.data.remote.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class CommentDto(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    val editedAt: Long? = null
)

@IgnoreExtraProperties
data class ReplyDto(
    val id: String = "",
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val mentionedUser: String? = null,
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    val editedAt: Long? = null
)
