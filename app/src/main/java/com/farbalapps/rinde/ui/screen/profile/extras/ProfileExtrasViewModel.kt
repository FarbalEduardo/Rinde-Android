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
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userId = sessionManager.userId
    
    val savedPosts: StateFlow<List<CommunityPost>> = _userId.flatMapLatest { id ->
        if (id.isNotEmpty()) getSavedPostsUseCase(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<List<Profile>> = _userId.flatMapLatest { id ->
        if (id.isNotEmpty()) getBlockedUsersUseCase(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            val id = _userId.first()
            if (id.isNotEmpty()) {
                unblockUserUseCase(id, targetUserId)
            }
        }
    }

    fun unsavePost(postId: String) {
        viewModelScope.launch {
            val id = _userId.first()
            if (id.isNotEmpty()) {
                toggleSavePostUseCase(id, postId, false)
            }
        }
    }
}
