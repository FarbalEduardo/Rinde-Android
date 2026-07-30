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
import com.farbalapps.rinde.domain.usecase.DeleteCommentUseCase
import com.farbalapps.rinde.domain.usecase.EditCommentUseCase
import com.farbalapps.rinde.domain.usecase.DeleteReplyUseCase
import com.farbalapps.rinde.domain.usecase.EditReplyUseCase
import com.farbalapps.rinde.domain.usecase.ReportCommentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.farbalapps.rinde.domain.usecase.VoteResult

import kotlinx.coroutines.flow.combine

enum class VoteUiState { IDLE, SENDING, ERROR, OFFLINE }

data class PostDetailUiState(
    val post: CommunityPost? = null,
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
    val error: String? = null,
    val editingCommentId: String? = null,
    val editingReplyId: String? = null,
    val editingText: String = "",
    val replyingToComment: Comment? = null,
    val replyText: String = "",
    val voteState: VoteUiState = VoteUiState.IDLE,
    val isVotePending: Boolean = false,
    val voteErrorMessage: String? = null,
    val isNewCommentFocused: Boolean = false
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val toggleVoteUseCase: ToggleVoteUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val addReplyUseCase: AddReplyUseCase,
    private val toggleLikeUseCase: ToggleCommentLikeUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val editCommentUseCase: EditCommentUseCase,
    private val deleteReplyUseCase: DeleteReplyUseCase,
    private val editReplyUseCase: EditReplyUseCase,
    private val reportCommentUseCase: ReportCommentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()
    private val currentUserId = currentUser?.id ?: ""
    private var currentPostId: String? = null
    private var postJob: kotlinx.coroutines.Job? = null

    init {
        _uiState.update { it.copy(currentUserId = currentUserId, currentUserName = currentUser?.displayName ?: "") }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Post Loading
    // ─────────────────────────────────────────────────────────────────────────

    fun loadPost(postId: String) {
        currentPostId = postId
        postJob?.cancel()
        postJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPost = true) }
            
            try {
                combine(
                    feedRepository.getPostById(postId),
                    feedRepository.globalPostStatus,
                    feedRepository.globalSavedStatus,
                    feedRepository.globalVoteStatus
                ) { post, postStatusOverlay, savedStatusOverlay, voteStatusOverlay ->
                    val overriddenStatus = postStatusOverlay[post.id] ?: post.verificationStatus
                    val overriddenSaved = savedStatusOverlay[post.id] ?: post.isSavedByMe
                    val voteOverlay = voteStatusOverlay[post.id]
                    post.copy(
                        verificationStatus = overriddenStatus,
                        isSavedByMe = overriddenSaved,
                        truthCount = voteOverlay?.truthCount ?: post.truthCount,
                        falseCount = voteOverlay?.falseCount ?: post.falseCount,
                        myVoteValue = voteOverlay?.myVote ?: post.myVoteValue
                    )
                }.collect { finalPost ->
                    _uiState.update { it.copy(post = finalPost, isLoadingPost = false) }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isLoadingPost = false, error = e.message) }
                }
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
    // Comment / Reply Actions
    // ─────────────────────────────────────────────────────────────────────────

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

    fun onNewCommentInputFocused() {
        _uiState.update { it.copy(isNewCommentFocused = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isNewCommentFocused = false) }
        }
    }

    fun saveEditedContent() {
        val postId = currentPostId ?: return
        val state = _uiState.value
        val text = state.editingText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            if (state.editingCommentId != null) {
                // El authorId lo valida el UseCase internamente usando currentUser
                editCommentUseCase(postId, state.editingCommentId, state.currentUserId, text)
            } else if (state.editingReplyId != null) {
                // Para simplificar encontramos el commentId buscando en replies
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
            deleteCommentUseCase(postId, commentId, authorId).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Comentario eliminado") }
            }
        }
    }

    fun deleteReply(commentId: String, replyId: String, authorId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            deleteReplyUseCase(commentId, replyId, postId, authorId).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Respuesta eliminada") }
            }
        }
    }

    fun setReplyingTo(comment: Comment?) {
        _uiState.update { it.copy(replyingToComment = comment, replyText = "") }
    }

    fun onReplyTextChange(text: String) {
        _uiState.update { it.copy(replyText = text) }
    }

    fun submitReply() {
        val postId = currentPostId ?: return
        val state = _uiState.value
        val comment = state.replyingToComment ?: return
        val text = state.replyText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }
            val result = addReplyUseCase(commentId = comment.id, postId = postId, text = text, imageUri = null)
            if (result.isSuccess) {
                _uiState.update { it.copy(replyText = "", replyingToComment = null, isSendingComment = false) }
                loadReplies(comment.id) // Reload replies for this comment
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isSendingComment = false) }
            }
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Voting
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleVote(voteValue: Int) {
        val post = _uiState.value.post ?: return
        if (_uiState.value.voteState == VoteUiState.SENDING) return

        val currentVote = post.myVoteValue
        val nextVote = if (currentVote == voteValue) 0 else voteValue

        _uiState.update { it.copy(voteState = VoteUiState.SENDING, voteErrorMessage = null) }

        viewModelScope.launch {
            // 2. Enviar voto (el UseCase maneja la persistencia en Room)
            when (val result = toggleVoteUseCase(post.id, voteValue, post.authorId)) {
                is VoteResult.Success -> {
                    result.counts?.let { (truth, false_, score) ->
                        _uiState.update { state ->
                            state.copy(
                                post = state.post?.copy(
                                    truthCount = truth,
                                    falseCount = false_,
                                    votesScore = score,
                                    myVoteValue = nextVote
                                ),
                                voteState = VoteUiState.IDLE,
                                isVotePending = false,
                                voteErrorMessage = null
                            )
                        }
                    } ?: _uiState.update { state ->
                        state.copy(
                            post = state.post?.copy(myVoteValue = nextVote),
                            voteState = VoteUiState.IDLE
                        )
                    }
                }

                is VoteResult.Offline -> {
                    _uiState.update {
                        it.copy(
                            voteState = VoteUiState.OFFLINE,
                            isVotePending = true,
                            voteErrorMessage = "Sin conexión. Tu voto se enviará cuando vuelva internet."
                        )
                    }
                }

                is VoteResult.NetworkError -> {
                    feedRepository.savePendingVote(currentUserId, post.id, voteValue, post.authorId)
                    _uiState.update {
                        it.copy(
                            voteState = VoteUiState.OFFLINE,
                            isVotePending = true,
                            voteErrorMessage = result.message
                        )
                    }
                }

                is VoteResult.ServerError -> {
                    _uiState.update {
                        it.copy(
                            post = post, // Revertir
                            voteState = VoteUiState.ERROR,
                            voteErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save / Like
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleSave() {
        val post = _uiState.value.post ?: return
        viewModelScope.launch {
            feedRepository.toggleSave(currentUserId, post.id)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(snackbarMessage = "No se pudo guardar la publicación. Intenta de nuevo.")
                    }
                }
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

    fun reportComment(comment: Comment) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reportCommentUseCase(
                postId = postId,
                commentId = comment.id,
                commentText = comment.text,
                authorId = comment.authorId
            ).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Comentario reportado") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun reportReply(reply: Reply) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reportCommentUseCase(
                postId = postId,
                commentId = reply.id,
                commentText = reply.text,
                authorId = reply.authorId
            ).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Respuesta reportada") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
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
        feedRepository.updatePostStatusLocal(postId, com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED)
        viewModelScope.launch {
            feedRepository.markPostAsExpired(postId)
        }
    }

    fun markAsAvailable() {
        val postId = currentPostId ?: return
        feedRepository.updatePostStatusLocal(postId, com.farbalapps.rinde.domain.model.VerificationStatus.PENDING)
        viewModelScope.launch {
            feedRepository.markPostAsAvailable(postId)
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
