package com.farbalapps.rinde.ui.screen.home.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.usecase.AddCommentUseCase
import com.farbalapps.rinde.domain.usecase.AddReplyUseCase
import com.farbalapps.rinde.domain.usecase.GetCommentsUseCase
import com.farbalapps.rinde.domain.usecase.ToggleCommentLikeUseCase
import com.farbalapps.rinde.domain.usecase.DeleteCommentUseCase
import com.farbalapps.rinde.domain.usecase.EditCommentUseCase
import com.farbalapps.rinde.domain.usecase.DeleteReplyUseCase
import com.farbalapps.rinde.domain.usecase.EditReplyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val replies: Map<String, List<Reply>> = emptyMap(), // commentId -> replies
    val isLoading: Boolean = false,
    val error: String? = null,
    val commentText: String = "",
    val selectedImageUri: Uri? = null,
    val replyingTo: Comment? = null,
    val editingCommentId: String? = null,
    val editingReplyId: String? = null,
    val editingText: String = "",
    val currentUserId: String = "" // Simplification for CommentsViewModel
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val addReplyUseCase: AddReplyUseCase,
    private val toggleLikeUseCase: ToggleCommentLikeUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val editCommentUseCase: EditCommentUseCase,
    private val deleteReplyUseCase: DeleteReplyUseCase,
    private val editReplyUseCase: EditReplyUseCase,
    private val authRepository: com.farbalapps.rinde.domain.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()
    private val currentUserId = currentUser?.id ?: ""
    private var currentPostId: String? = null

    init {
        _uiState.update { it.copy(currentUserId = currentUserId) }
    }

    fun loadComments(postId: String) {
        currentPostId = postId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCommentsUseCase.getComments(postId).collect { comments ->
                _uiState.update { it.copy(comments = comments, isLoading = false) }
            }
        }
    }

    fun loadReplies(commentId: String) {
        viewModelScope.launch {
            getCommentsUseCase.getReplies(commentId).collect { replies ->
                _uiState.update { state ->
                    val newReplies = state.replies.toMutableMap()
                    newReplies[commentId] = replies
                    state.copy(replies = newReplies)
                }
            }
        }
    }

    fun onCommentTextChange(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun setReplyingTo(comment: Comment?) {
        _uiState.update { it.copy(replyingTo = comment, commentText = "") }
    }

    fun startEditComment(comment: Comment) {
        _uiState.update { it.copy(editingCommentId = comment.id, editingText = comment.text, editingReplyId = null) }
    }

    fun startEditReply(reply: Reply) {
        _uiState.update { it.copy(editingReplyId = reply.id, editingText = reply.text, editingCommentId = null) }
    }

    fun onEditTextChange(text: String) {
        _uiState.update { it.copy(editingText = text) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingCommentId = null, editingReplyId = null, editingText = "") }
    }

    fun saveEditedContent() {
        val postId = currentPostId ?: return
        val state = _uiState.value
        val text = state.editingText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            if (state.editingCommentId != null) {
                editCommentUseCase(postId, state.editingCommentId, state.currentUserId, text)
            } else if (state.editingReplyId != null) {
                var parentCommentId = ""
                for ((cId, repls) in state.replies) {
                    if (repls.any { it.id == state.editingReplyId }) {
                        parentCommentId = cId
                        break
                    }
                }
                if (parentCommentId.isNotEmpty()) {
                    editReplyUseCase(parentCommentId, state.editingReplyId, state.currentUserId, text)
                }
            }
            cancelEdit()
        }
    }

    fun deleteComment(commentId: String, authorId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            deleteCommentUseCase(postId, commentId, authorId)
        }
    }

    fun deleteReply(commentId: String, replyId: String, authorId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            deleteReplyUseCase(commentId, replyId, postId, authorId)
        }
    }

    fun submitComment() {
        val postId = currentPostId ?: return
        val state = _uiState.value
        if (state.commentText.isBlank() && state.selectedImageUri == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = if (state.replyingTo != null) {
                addReplyUseCase(
                    commentId = state.replyingTo.id,
                    postId = postId,
                    text = state.commentText,
                    imageUri = state.selectedImageUri
                )
            } else {
                addCommentUseCase(
                    postId = postId,
                    text = state.commentText,
                    imageUri = state.selectedImageUri
                )
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(
                    commentText = "",
                    selectedImageUri = null,
                    replyingTo = null,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(
                    error = result.exceptionOrNull()?.message,
                    isLoading = false
                ) }
            }
        }
    }

    fun toggleLike(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            toggleLikeUseCase.toggleCommentLike(postId, commentId)
        }
    }

    fun toggleReplyLike(commentId: String, replyId: String) {
        viewModelScope.launch {
            toggleLikeUseCase.toggleReplyLike(commentId, replyId)
        }
    }
}
