package com.farbalapps.rinde.ui.screen.profile.extras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.usecase.profile.GetBlockedUsersUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetSavedPostsUseCase
import com.farbalapps.rinde.domain.usecase.profile.UnblockUserUseCase
import com.farbalapps.rinde.domain.usecase.profile.ToggleSavePostUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileExtrasViewModel @Inject constructor(
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val sessionManager: SessionManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userId = sessionManager.userId
    
    private val _isSavedLoading = MutableStateFlow(true)
    val isSavedLoading: StateFlow<Boolean> = _isSavedLoading.asStateFlow()

    private val _isBlockedLoading = MutableStateFlow(true)
    val isBlockedLoading: StateFlow<Boolean> = _isBlockedLoading.asStateFlow()

    val savedPosts: StateFlow<List<CommunityPost>> = _userId
        .onStart {
            val currentUid = firebaseAuth.currentUser?.uid.orEmpty()
            if (currentUid.isNotEmpty()) emit(currentUid)
        }
        .flatMapLatest { id ->
            val uid = id.ifEmpty { firebaseAuth.currentUser?.uid.orEmpty() }
            if (uid.isNotEmpty()) {
                getSavedPostsUseCase(uid).onEach {
                    _isSavedLoading.value = false
                }
            } else {
                _isSavedLoading.value = false
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<List<Profile>> = _userId
        .onStart {
            val currentUid = firebaseAuth.currentUser?.uid.orEmpty()
            if (currentUid.isNotEmpty()) emit(currentUid)
        }
        .flatMapLatest { id ->
            val uid = id.ifEmpty { firebaseAuth.currentUser?.uid.orEmpty() }
            if (uid.isNotEmpty()) {
                getBlockedUsersUseCase(uid).onEach {
                    _isBlockedLoading.value = false
                }
            } else {
                _isBlockedLoading.value = false
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            val id = firebaseAuth.currentUser?.uid ?: _userId.first()
            if (id.isNotEmpty()) {
                unblockUserUseCase(id, targetUserId)
            }
        }
    }

    fun unsavePost(postId: String) {
        viewModelScope.launch {
            val id = firebaseAuth.currentUser?.uid ?: _userId.first()
            if (id.isNotEmpty()) {
                toggleSavePostUseCase(id, postId, false)
            }
        }
    }
}
