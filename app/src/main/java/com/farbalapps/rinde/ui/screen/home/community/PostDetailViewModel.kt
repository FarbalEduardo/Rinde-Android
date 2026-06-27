package com.farbalapps.rinde.ui.screen.home.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.AddCommentUseCase
import com.farbalapps.rinde.domain.usecase.AddReplyUseCase
import com.farbalapps.rinde.domain.usecase.GetCommentsUseCase
import com.farbalapps.rinde.domain.usecase.ToggleCommentLikeUseCase
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostDetailUiState(
    val post: CommunityPost? = null,
    // Comentarios ordenados de más reciente a más antiguo
    val comments: List<Comment> = emptyList(),
    val replies: Map<String, List<Reply>> = emptyMap(),
    val isLoadingPost: Boolean = true,
    val isLoadingComments: Boolean = false,
    val isSendingComment: Boolean = false,
    val commentText: String = "",
    val currentUserId: String = "",
    val currentUserName: String = "",
    val isDeleted: Boolean = false,
    val snackbarMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val toggleVoteUseCase: ToggleVoteUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val addReplyUseCase: AddReplyUseCase,
    private val toggleLikeUseCase: ToggleCommentLikeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()
    private val currentUserId = currentUser?.id ?: ""
    private var currentPostId: String? = null

    init {
        _uiState.update { it.copy(currentUserId = currentUserId, currentUserName = currentUser?.displayName ?: "") }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Post Loading
    // ─────────────────────────────────────────────────────────────────────────

    fun loadPost(postId: String) {
        currentPostId = postId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPost = true) }
            try {
                feedRepository.getPostById(postId).collect { post ->
                    _uiState.update { it.copy(post = post, isLoadingPost = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingPost = false, error = e.message) }
            }
        }
        loadComments(postId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comments — ordered newest first
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadComments(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            try {
                getCommentsUseCase.getComments(postId).collect { comments ->
                    // Ordenar de más reciente a más antiguo por timestamp
                    val sorted = comments.sortedByDescending { it.timestamp }
                    _uiState.update { it.copy(comments = sorted, isLoadingComments = false) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PostDetailVM", "Error cargando comentarios: ${e.message}")
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    fun loadReplies(commentId: String) {
        viewModelScope.launch {
            try {
                getCommentsUseCase.getReplies(commentId).collect { replies ->
                    // Replies también de más reciente a más antiguo
                    val sorted = replies.sortedByDescending { it.timestamp }
                    _uiState.update { state ->
                        val updated = state.replies.toMutableMap()
                        updated[commentId] = sorted
                        state.copy(replies = updated)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PostDetailVM", "Error cargando respuestas: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comment Input
    // ─────────────────────────────────────────────────────────────────────────

    fun onCommentTextChange(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    fun submitComment() {
        val postId = currentPostId ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }
            val result = addCommentUseCase(postId = postId, text = text, imageUri = null)
            if (result.isSuccess) {
                _uiState.update { it.copy(commentText = "", isSendingComment = false) }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message,
                        isSendingComment = false
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Voting
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleVote(voteValue: Int) {
        val post = _uiState.value.post ?: return
        // Optimistic update
        val currentVote = post.myVoteValue
        val updatedPost = if (currentVote == voteValue) {
            val scoreDiff = -voteValue
            val newTruth = if (voteValue > 0) post.truthCount - 1 else post.truthCount
            val newFalse = if (voteValue < 0) post.falseCount - 1 else post.falseCount
            post.copy(myVoteValue = 0, votesScore = post.votesScore + scoreDiff,
                truthCount = newTruth, falseCount = newFalse)
        } else {
            val scoreDiff = if (currentVote == 0) voteValue else voteValue - currentVote
            val newTruth = post.truthCount + (if (voteValue > 0) 1 else 0) - (if (currentVote > 0) 1 else 0)
            val newFalse = post.falseCount + (if (voteValue < 0) 1 else 0) - (if (currentVote < 0) 1 else 0)
            post.copy(myVoteValue = voteValue, votesScore = post.votesScore + scoreDiff,
                truthCount = newTruth, falseCount = newFalse)
        }
        _uiState.update { it.copy(post = updatedPost) }

        viewModelScope.launch {
            toggleVoteUseCase(post.id, voteValue, post.authorId)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save / Like
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleSave() {
        val post = _uiState.value.post ?: return
        _uiState.update { it.copy(post = post.copy(isSavedByMe = !post.isSavedByMe)) }
        viewModelScope.launch {
            feedRepository.toggleSave(currentUserId, post.id)
        }
    }

    fun toggleCommentLike(commentId: String) {
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun deletePost() {
        val post = _uiState.value.post ?: return
        val postId = currentPostId ?: return
        viewModelScope.launch {
            feedRepository.deletePost(postId, post.photos).onSuccess {
                _uiState.update { it.copy(isDeleted = true, snackbarMessage = "Publicación eliminada") }
            }
        }
    }

    fun markAsExpired() {
        val postId = currentPostId ?: return
        _uiState.update { state -> 
            val updatedPost = state.post?.copy(verificationStatus = com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED)
            state.copy(post = updatedPost)
        }
        viewModelScope.launch {
            feedRepository.markPostAsExpired(postId)
        }
    }

    fun reportAsExpired() {
        val post = _uiState.value.post ?: return
        viewModelScope.launch {
            feedRepository.reportPostAsExpired(
                postId = post.id,
                postTitle = post.title,
                authorId = post.authorId,
                currentUserId = _uiState.value.currentUserId,
                currentUserName = _uiState.value.currentUserName
            ).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Reporte enviado al autor") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
