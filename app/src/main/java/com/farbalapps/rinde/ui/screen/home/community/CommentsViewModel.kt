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
    val replyingTo: Comment? = null
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val addReplyUseCase: AddReplyUseCase,
    private val toggleLikeUseCase: ToggleCommentLikeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private var currentPostId: String? = null

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
        _uiState.update { it.copy(replyingTo = comment) }
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
