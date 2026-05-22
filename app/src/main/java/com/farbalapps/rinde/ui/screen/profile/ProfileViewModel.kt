package com.farbalapps.rinde.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.usecase.profile.GetProfilePostsUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.FollowUserUseCase
import com.farbalapps.rinde.domain.usecase.profile.UnfollowUserUseCase
import com.farbalapps.rinde.domain.usecase.profile.IsFollowingUseCase
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
    val savedPosts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = true,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePostsUseCase: GetProfilePostsUseCase,
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val isFollowingUseCase: IsFollowingUseCase,
    private val updatePrivacyUseCase: UpdatePrivacyUseCase,
    private val syncProfileUseCase: SyncProfileUseCase,
    private val clearUploadStatusUseCase: ClearUploadStatusUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var targetUserId: String? = null

    init {
        startDataObserving()
    }

    private fun startDataObserving() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            loadAllData(currentUser.uid)
        } else {
            var listener: FirebaseAuth.AuthStateListener? = null
            listener = FirebaseAuth.AuthStateListener { auth ->
                auth.currentUser?.let { user ->
                    loadAllData(user.uid)
                    listener?.let { firebaseAuth.removeAuthStateListener(it) }
                }
            }
            firebaseAuth.addAuthStateListener(listener)
        }
    }

    private fun loadAllData(uid: String) {
        this.targetUserId = uid
        _uiState.update { it.copy(isCurrentUser = true, isLoading = true, error = null) }

        // 1. Single Source of Truth: Observamos Room (Capa de Dominio)
        viewModelScope.launch {
            getProfileUseCase(uid)
                .catch { e ->
                    _uiState.update { it.copy(error = "Error local: ${e.message}", isLoading = false) }
                }
                .collect { profile ->
                    _uiState.update { 
                        it.copy(
                            profile = profile, 
                            isLoading = profile.isDummy // Si es dummy, sigue cargando
                        ) 
                    }
                }
        }

        // 2. Fetch remoto: Traemos de Firebase y actualizamos Room (SSOT)
        viewModelScope.launch {
            try {
                syncProfileUseCase(uid)
            } catch (e: Exception) {
                // Solo mostramos error si el perfil local también es dummy (no hay datos offline)
                if (_uiState.value.profile?.isDummy == true) {
                    _uiState.update { it.copy(error = "Error de conexión: ${e.localizedMessage}", isLoading = false) }
                }
            }
        }

        // 3. Obtener posts
        viewModelScope.launch {
            getProfilePostsUseCase(uid)
                .catch { e ->
                    android.util.Log.e("ProfileViewModel", "Error fetching posts", e)
                }
                .collect { posts ->
                    _uiState.update { it.copy(posts = posts) }
                }
        }

        // 4. Obtener posts guardados
        viewModelScope.launch {
            getSavedPostsUseCase(uid)
                .catch { e ->
                    android.util.Log.e("ProfileViewModel", "Error fetching saved posts", e)
                }
                .collect { savedPosts ->
                    _uiState.update { it.copy(savedPosts = savedPosts) }
                }
        }
    }

    fun toggleFollow() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        val targetId = this.targetUserId ?: return
        if (currentUserId == targetId) return
        
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                unfollowUserUseCase(currentUserId, targetId)
            } else {
                followUserUseCase(currentUserId, targetId)
            }
        }
    }

    fun retry() {
        startDataObserving()
    }

    fun clearUploadStatus() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            clearUploadStatusUseCase(uid)
        }
    }
}
