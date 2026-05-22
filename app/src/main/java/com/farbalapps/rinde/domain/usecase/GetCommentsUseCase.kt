package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    fun getComments(postId: String): Flow<List<Comment>> {
        return commentRepository.getComments(postId)
    }

    fun getReplies(commentId: String): Flow<List<Reply>> {
        return commentRepository.getReplies(commentId)
    }
}
