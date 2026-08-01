package com.farbalapps.rinde.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfilePostsUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.UpdatePrivacyUseCase
import com.farbalapps.rinde.domain.usecase.profile.SyncProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.ClearUploadStatusUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetSavedPostsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: Profile? = null,
    val posts: List<CommunityPost> = emptyList(),
    val computedRating: Float? = null,
    val ratedPostsCount: Int = 0,
    val isLoading: Boolean = true,
    val isCurrentUser: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePostsUseCase: GetProfilePostsUseCase,
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val updatePrivacyUseCase: UpdatePrivacyUseCase,
    private val syncProfileUseCase: SyncProfileUseCase,
    private val clearUploadStatusUseCase: ClearUploadStatusUseCase,
    private val toggleVoteUseCase: ToggleVoteUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val postStatusOverlay = feedRepository.globalPostStatus
    val savedStatusOverlay = feedRepository.globalSavedStatus
    val voteStatusOverlay = feedRepository.globalVoteStatus

    private var targetUserId: String? = null

    init {
        startDataObserving()
    }

    private fun startDataObserving() {
        // Will be called from UI layer with the targetUserId
    }

    fun loadProfile(targetUid: String?) {
        val currentUserId = firebaseAuth.currentUser?.uid
        val finalUid = targetUid ?: currentUserId ?: return

        this.targetUserId = finalUid
        val isMe = (currentUserId == finalUid)
        
        _uiState.update { it.copy(isCurrentUser = isMe, isLoading = true, error = null) }

        if (isMe) {
            syncCurrentUser(finalUid)
        }

        observeLocalProfile(finalUid)
        syncRemoteProfile(finalUid)
        observeProfilePosts(finalUid)
    }

    private fun syncCurrentUser(uid: String) {
        viewModelScope.launch {
            feedRepository.syncUserVotes(uid)
            feedRepository.syncUserSavedPosts(uid)
        }
    }

    private fun observeLocalProfile(uid: String) {
        viewModelScope.launch {
            getProfileUseCase(uid)
                .catch { e ->
                    _uiState.update { it.copy(error = "Error local: ${e.message}", isLoading = false) }
                }
                .collect { profile ->
                    _uiState.update { state ->
                        state.copy(
                            profile = profile, 
                            isLoading = profile.isDummy && state.error == null
                        ) 
                    }
                }
        }
    }

    private fun syncRemoteProfile(uid: String) {
        viewModelScope.launch {
            try {
                syncProfileUseCase(uid)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Sync failed for $uid", e)
                val currentProfile = _uiState.value.profile
                if (currentProfile == null || currentProfile.isDummy) {
                    _uiState.update { it.copy(
                        error = "Error de conexión: ${e.localizedMessage}", 
                        isLoading = false 
                    ) }
                }
            }
        }
    }

    private fun observeProfilePosts(uid: String) {
        viewModelScope.launch {
            getProfilePostsUseCase(uid)
                .catch { e ->
                    android.util.Log.e("ProfileViewModel", "Error fetching posts", e)
                }
                .collect { posts ->
                    val (rating, count) = calculateCommunityRating(posts)
                    _uiState.update { it.copy(posts = posts, computedRating = rating, ratedPostsCount = count) }
                }
        }
    }

    private fun calculateCommunityRating(posts: List<CommunityPost>): Pair<Float?, Int> {
        val eligiblePosts = posts.filter { 
            (it.truthCount + it.falseCount) >= com.farbalapps.rinde.domain.model.VerdictCalculator.MIN_VOTES_THRESHOLD 
        }
        return if (eligiblePosts.isNotEmpty()) {
            val avgRatio = eligiblePosts.map { 
                it.truthCount.toFloat() / (it.truthCount + it.falseCount) 
            }.average().toFloat()
            val stars = (1f + avgRatio * 4f).coerceIn(1f, 5f)
            stars to eligiblePosts.size
        } else {
            null to 0
        }
    }

    fun retry() {
        loadProfile(this.targetUserId)
    }

    fun clearUploadStatus() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            clearUploadStatusUseCase(uid)
        }
    }

    fun toggleVote(postId: String, voteValue: Int) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        val currentVote = post.myVoteValue
        val nextVote = if (currentVote == voteValue) 0 else voteValue

        _uiState.update { state ->
            val updatedPosts = state.posts.map { p ->
                if (p.id == postId) {
                    p.copy(myVoteValue = nextVote)
                } else p
            }
            state.copy(posts = updatedPosts)
        }

        viewModelScope.launch {
            val authorId = post.authorId
            toggleVoteUseCase(postId, voteValue, authorId)
        }
    }

    fun toggleSave(postId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            feedRepository.toggleSave(currentUserId, postId)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(snackbarMessage = "No se pudo guardar la publicación. Intenta de nuevo.")
                    }
                }
        }
    }

    fun deletePost(postId: String, photoUrls: List<String>) {
        viewModelScope.launch {
            feedRepository.deletePost(postId, photoUrls).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Publicación eliminada") }
            }
        }
    }

    fun markAsExpired(postId: String) {
        feedRepository.updatePostStatusLocal(postId, com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED)
        viewModelScope.launch {
            feedRepository.markPostAsExpired(postId)
        }
    }

    fun markAsAvailable(postId: String) {
        feedRepository.updatePostStatusLocal(postId, com.farbalapps.rinde.domain.model.VerificationStatus.PENDING)
        viewModelScope.launch {
            feedRepository.markPostAsAvailable(postId)
        }
    }

    fun reportAsExpired(postId: String, postTitle: String, authorId: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        viewModelScope.launch {
            feedRepository.reportPostAsExpired(
                postId = postId,
                postTitle = postTitle,
                authorId = authorId,
                currentUserId = currentUser.uid,
                currentUserName = currentUser.displayName ?: "Usuario"
            ).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Reporte enviado al autor") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
