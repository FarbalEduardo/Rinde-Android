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

        // Sincronizar votos si es mi propio perfil para asegurar que el caché esté fresco
        if (isMe) {
            viewModelScope.launch {
                feedRepository.syncUserVotes(finalUid)
                feedRepository.syncUserSavedPosts(finalUid)
            }
        }

        // 1. Single Source of Truth: Observamos Room (Capa de Dominio)
        viewModelScope.launch {
            getProfileUseCase(finalUid)
                .catch { e ->
                    _uiState.update { it.copy(error = "Error local: ${e.message}", isLoading = false) }
                }
                .collect { profile ->
                    _uiState.update { state ->
                        state.copy(
                            profile = profile, 
                            // Si es dummy y no hay error, seguimos esperando al sync remoto
                            isLoading = profile.isDummy && state.error == null
                        ) 
                    }
                }
        }

        // 2. Fetch remoto: Traemos de Firebase y actualizamos Room (SSOT)
        viewModelScope.launch {
            try {
                syncProfileUseCase(finalUid)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Sync failed for $finalUid", e)
                // Obligamos a apagar loading si hay error de red y no tenemos datos reales
                val currentProfile = _uiState.value.profile
                if (currentProfile == null || currentProfile.isDummy) {
                    _uiState.update { it.copy(
                        error = "Error de conexión: ${e.localizedMessage}", 
                        isLoading = false 
                    ) }
                }
            }
        }

        // 3. Obtener posts
        viewModelScope.launch {
            getProfilePostsUseCase(finalUid)
                .catch { e ->
                    android.util.Log.e("ProfileViewModel", "Error fetching posts", e)
                }
                .collect { posts ->
                    _uiState.update { it.copy(posts = posts) }
                }
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
        // Actualización optimista en la lista local
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val currentVote = post.myVoteValue
                    if (currentVote == voteValue) {
                        val scoreDiff = -voteValue
                        val newTruth = if (voteValue > 0) post.truthCount - 1 else post.truthCount
                        val newFalse = if (voteValue < 0) post.falseCount - 1 else post.falseCount
                        post.copy(myVoteValue = 0, votesScore = post.votesScore + scoreDiff, truthCount = newTruth, falseCount = newFalse)
                    } else {
                        val scoreDiff = if (currentVote == 0) voteValue else voteValue - currentVote
                        val newTruth = post.truthCount + (if (voteValue > 0) 1 else 0) - (if (currentVote > 0) 1 else 0)
                        val newFalse = post.falseCount + (if (voteValue < 0) 1 else 0) - (if (currentVote < 0) 1 else 0)
                        post.copy(myVoteValue = voteValue, votesScore = post.votesScore + scoreDiff, truthCount = newTruth, falseCount = newFalse)
                    }
                } else post
            }
            state.copy(posts = updatedPosts)
        }
        viewModelScope.launch {
            val authorId = _uiState.value.posts.find { it.id == postId }?.authorId ?: return@launch
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
