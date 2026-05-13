package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getComments(postId: String): Flow<List<Comment>>
    fun getReplies(commentId: String): Flow<List<Reply>>
    suspend fun addComment(postId: String, comment: Comment, localImageUri: android.net.Uri?): Result<Unit>
    suspend fun addReply(commentId: String, reply: Reply, localImageUri: android.net.Uri?): Result<Unit>
    suspend fun toggleCommentLike(userId: String, postId: String, commentId: String): Result<Unit>
    suspend fun toggleReplyLike(userId: String, commentId: String, replyId: String): Result<Unit>
}
