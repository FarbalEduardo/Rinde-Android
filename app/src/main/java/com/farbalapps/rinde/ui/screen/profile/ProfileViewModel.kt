package com.farbalapps.rinde.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.ProfilePost
import com.farbalapps.rinde.domain.usecase.profile.GetProfilePostsUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: Profile? = null,
    val posts: List<ProfilePost> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePostsUseCase: GetProfilePostsUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        val userId = firebaseAuth.currentUser?.uid ?: "dummy_user_id"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            launch {
                getProfileUseCase(userId)
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                    .collect { profile ->
                        _uiState.update { it.copy(profile = profile, isLoading = false) }
                    }
            }

            launch {
                getProfilePostsUseCase(userId)
                    .catch { e ->
                        // non-fatal
                    }
                    .collect { posts ->
                        _uiState.update { it.copy(posts = posts) }
                    }
            }
        }
    }
}
