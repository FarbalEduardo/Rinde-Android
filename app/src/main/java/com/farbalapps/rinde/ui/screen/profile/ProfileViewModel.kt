package com.farbalapps.rinde.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import com.farbalapps.rinde.domain.usecase.profile.CalculateCommunityRatingUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfilePostsUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.UpdatePrivacyUseCase
import com.farbalapps.rinde.domain.usecase.profile.SyncProfileUseCase
import com.farbalapps.rinde.domain.usecase.profile.ClearUploadStatusUseCase
import com.farbalapps.rinde.domain.usecase.profile.GetSavedPostsUseCase
import com.farbalapps.rinde.data.local.AppLanguage
import com.farbalapps.rinde.data.local.ThemeMode
import com.farbalapps.rinde.domain.usecase.settings.*
import com.farbalapps.rinde.util.UiText
import com.farbalapps.rinde.util.logger.AppLogger
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: Profile? = null,
    val posts: List<CommunityPost> = emptyList(),
    val computedRating: Float? = null,
    val ratedPostsCount: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isCurrentUser: Boolean = false,
    val isPrivateProfileRestricted: Boolean = false,
    val error: UiText? = null,
    val snackbarMessage: UiText? = null
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
    private val calculateCommunityRatingUseCase: CalculateCommunityRatingUseCase,
    private val getThemeUseCase: GetThemeUseCase,
    private val setThemeUseCase: SetThemeUseCase,
    private val getLanguageUseCase: GetLanguageUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val isProfilePrivateUseCase: IsProfilePrivateUseCase,
    private val togglePrivacyUseCase: TogglePrivacyUseCase,
    private val settingsManager: com.farbalapps.rinde.data.local.SettingsManager,
    private val firebaseAuth: FirebaseAuth,
    private val feedRepository: FeedRepository,
    private val logger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = getThemeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val appLanguage: StateFlow<AppLanguage> = getLanguageUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ES)

    val appCurrency: StateFlow<com.farbalapps.rinde.data.local.AppCurrency> = settingsManager.appCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.farbalapps.rinde.data.local.AppCurrency.CLP)

    val isBunkerMode: StateFlow<Boolean> = settingsManager.isBunkerMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isProfilePrivate: StateFlow<Boolean> = isProfilePrivateUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val postStatusOverlay = feedRepository.globalPostStatus
    val savedStatusOverlay = feedRepository.globalSavedStatus
    val voteStatusOverlay = feedRepository.globalVoteStatus

    private var targetUserId: String? = null
    private var profileJob: Job? = null
    private var postsJob: Job? = null
    private var syncJob: Job? = null

    fun loadProfile(targetUid: String?, isRefresh: Boolean = false) {
        val currentUserId = firebaseAuth.currentUser?.uid
        val finalUid = targetUid ?: currentUserId

        if (finalUid == null) {
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    isRefreshing = false, 
                    error = UiText.StringResource(R.string.profile_error_session)
                ) 
            }
            return
        }

        this.targetUserId = finalUid
        val isMe = (currentUserId == finalUid)
        
        _uiState.update { 
            it.copy(
                isCurrentUser = isMe, 
                isLoading = !isRefresh && it.profile == null,
                isRefreshing = isRefresh,
                error = null
            ) 
        }

        if (isMe) {
            syncCurrentUser(finalUid)
        }

        observeLocalProfile(finalUid, isMe)
        syncRemoteProfile(finalUid, isRefresh)
        observeProfilePosts(finalUid)
    }

    fun refreshProfile() {
        targetUserId?.let { uid ->
            loadProfile(uid, isRefresh = true)
        } ?: run {
            loadProfile(null, isRefresh = true)
        }
    }

    private fun syncCurrentUser(userId: String) {
        viewModelScope.launch {
            feedRepository.syncUserVotes(userId)
            feedRepository.syncUserSavedPosts(userId)
        }
    }

    private fun observeLocalProfile(userId: String, isMe: Boolean) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            getProfileUseCase(userId)
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            error = UiText.StringResource(R.string.profile_error_local, e.message ?: ""), 
                            isLoading = false,
                            isRefreshing = false
                        ) 
                    }
                }
                .collect { profile ->
                    val isRestricted = !isMe && profile.isPrivate
                    _uiState.update { state ->
                        state.copy(
                            profile = profile, 
                            isLoading = profile.isDummy && state.error == null,
                            isPrivateProfileRestricted = isRestricted
                        ) 
                    }
                }
        }
    }

    private fun syncRemoteProfile(userId: String, isRefresh: Boolean) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                syncProfileUseCase(userId)
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                logger.error(TAG, "Sync failed for $userId", e)
                val currentProfile = _uiState.value.profile
                _uiState.update { state ->
                    val hasData = currentProfile != null && !currentProfile.isDummy
                    state.copy(
                        isRefreshing = false,
                        isLoading = if (!hasData) false else state.isLoading,
                        error = if (!hasData) UiText.StringResource(R.string.profile_error_network, e.localizedMessage ?: "") else null
                    )
                }
            }
        }
    }

    private fun observeProfilePosts(userId: String) {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            getProfilePostsUseCase(userId)
                .catch { e ->
                    logger.error(TAG, "Error fetching posts", e)
                }
                .collect { posts ->
                    val (rating, count) = calculateCommunityRatingUseCase(posts)
                    _uiState.update { 
                        it.copy(
                            posts = posts, 
                            computedRating = rating, 
                            ratedPostsCount = count 
                        ) 
                    }
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
                        state.copy(snackbarMessage = UiText.StringResource(R.string.profile_save_post_error))
                    }
                }
        }
    }

    fun deletePost(postId: String, photoUrls: List<String>) {
        viewModelScope.launch {
            feedRepository.deletePost(postId, photoUrls).onSuccess {
                _uiState.update { it.copy(snackbarMessage = UiText.StringResource(R.string.profile_post_deleted)) }
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
                _uiState.update { it.copy(snackbarMessage = UiText.StringResource(R.string.profile_report_sent)) }
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeUseCase(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            setLanguageUseCase(language)
        }
    }

    fun togglePrivacy(isPrivate: Boolean) {
        viewModelScope.launch {
            togglePrivacyUseCase(isPrivate)
        }
    }

    fun setCurrency(currency: com.farbalapps.rinde.data.local.AppCurrency) {
        viewModelScope.launch {
            settingsManager.setAppCurrency(currency)
        }
    }

    fun toggleBunkerMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setBunkerMode(enabled)
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
