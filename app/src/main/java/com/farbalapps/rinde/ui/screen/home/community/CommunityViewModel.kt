package com.farbalapps.rinde.ui.screen.home.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

import com.farbalapps.rinde.util.LocationService

enum class CommunityTab {
    DISCOVER, FOLLOWING, SAVED, NEARBY
}

data class CommunityUiState(
    val posts: List<CommunityPost> = emptyList(),
    val currentTab: CommunityTab = CommunityTab.DISCOVER,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFirstLoad: Boolean = false,
    val userId: String = "",
    val userName: String = "",
    val lastPostId: String? = null,
    val unreadNotificationCount: Int = 0,
    val snackbarMessage: String? = null,
    val newPostsCount: Int = 0
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val toggleVoteUseCase: ToggleVoteUseCase,
    private val locationService: LocationService,
    private val syncMetadataDao: com.farbalapps.rinde.data.local.dao.SyncMetadataDao,
    private val postDao: com.farbalapps.rinde.data.local.dao.PostDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _userId = authRepository.getCurrentUser()?.id ?: ""

    private val _forceRefreshSharedFlow = MutableSharedFlow<Boolean>(replay = 1)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedFeed: Flow<PagingData<CommunityPost>> = _forceRefreshSharedFlow
        .onStart { emit(false) }
        .flatMapLatest { force ->
            feedRepository.getPagedFeed(forceRefresh = force)
        }
        .cachedIn(viewModelScope)

    private var feedJob: Job? = null

    init {
        val user = authRepository.getCurrentUser()
        _uiState.update { it.copy(userId = user?.id ?: "", userName = user?.displayName ?: "") }
        
        // Limpiar caché de Room (eliminar posts de más de 7 días para ahorrar espacio y optimizar recursos)
        viewModelScope.launch {
            try {
                val threshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                postDao.deleteOldPosts(threshold)
                android.util.Log.d("CommunityViewModel", "🧹 Room cache cleaned: deleted posts older than 7 days")
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "Failed to clean Room cache: ${e.message}")
            }
        }

        viewModelScope.launch {
            val hasCachedData = postDao.getPostsOnce(1).isNotEmpty()
            _uiState.update { it.copy(isFirstLoad = !hasCachedData) }
            if (!hasCachedData) {
                _forceRefreshSharedFlow.emit(true)
            }
        }

        viewModelScope.launch {
            _uiState.map { it.currentTab }.distinctUntilChanged().collectLatest { tab ->
                _uiState.update { it.copy(newPostsCount = 0) }
                if (tab == CommunityTab.DISCOVER) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    refresh(clearPosts = true)
                }
            }
        }

        // Listener de notificaciones
        if (_userId.isNotEmpty()) {
            viewModelScope.launch {
                feedRepository.getUnreadNotificationsCount(_userId)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5_000),
                        initialValue = 0
                    )
                    .collect { count ->
                        _uiState.update { it.copy(unreadNotificationCount = count) }
                    }
            }
        }

        // Polling loop cada 10 minutos
        viewModelScope.launch {
            while (true) {
                delay(10 * 60 * 1000L)
                if (_uiState.value.currentTab == CommunityTab.DISCOVER) {
                    _forceRefreshSharedFlow.emit(true)
                }
            }
        }
    }

    fun onFirstLoadComplete() {
        _uiState.update { it.copy(isFirstLoad = false) }
    }

    fun showPendingPosts() {
        _uiState.update { it.copy(newPostsCount = 0) }
        viewModelScope.launch {
            val maxTs = feedRepository.refreshFeedIfNeeded(forceRefresh = true).getOrDefault(0)
            updateLastSeenTimestamp()
            _forceRefreshSharedFlow.emit(true)
        }
    }

    private suspend fun updateLastSeenTimestamp() {
        val maxTs = feedRepository.refreshFeedIfNeeded(forceRefresh = false).getOrNull()
        val now = System.currentTimeMillis()
        val meta = syncMetadataDao.getMetadata("feed_global")
        if (meta != null) {
            syncMetadataDao.upsert(meta.copy(lastSeenTimestamp = now))
        } else {
            syncMetadataDao.upsert(
                com.farbalapps.rinde.data.local.entity.SyncMetadataEntity(
                    key = "feed_global",
                    lastSyncTimestamp = now,
                    lastSeenTimestamp = now
                )
            )
        }
    }

    fun checkForNewPostsOnResume() {
        if (_uiState.value.currentTab != CommunityTab.DISCOVER) return
        viewModelScope.launch {
            val meta = syncMetadataDao.getMetadata("feed_global")
            val lastSeen = meta?.lastSeenTimestamp ?: meta?.lastSyncTimestamp ?: 0L
            if (lastSeen > 0L) {
                val count = feedRepository.countNewPostsSince(lastSeen)
                if (count > 20) {
                    _uiState.update { it.copy(newPostsCount = count) }
                } else {
                    _uiState.update { it.copy(newPostsCount = 0) }
                }
            }
        }
    }

    private fun startFeedCollection() {
        feedJob?.cancel()
        _uiState.update { it.copy(newPostsCount = 0) }

        val tab = _uiState.value.currentTab
        if (tab == CommunityTab.DISCOVER) {
            return // Managed by Paging 3 flow
        }

        feedJob = viewModelScope.launch {
            val uid = _uiState.value.userId

            if (uid.isNotEmpty()) {
                feedRepository.syncUserVotes(uid)
                feedRepository.syncUserSavedPosts(uid)
            }

            val flow = when (tab) {
                CommunityTab.DISCOVER -> flowOf(emptyList())
                CommunityTab.FOLLOWING -> if (uid.isNotEmpty()) feedRepository.getFollowingFeed(uid, null) else flowOf(emptyList())
                CommunityTab.SAVED -> if (uid.isNotEmpty()) feedRepository.getSavedPosts(uid) else flowOf(emptyList())
                CommunityTab.NEARBY -> {
                    val location = locationService.getCurrentLocation()
                    if (location != null) {
                        feedRepository.getNearbyFeed(location.latitude, location.longitude, 10.0)
                    } else {
                        flowOf(emptyList())
                    }
                }
            }

            var firstEmission = true
            flow.collectLatest { newPosts ->
                _uiState.update { it.copy(
                    posts = newPosts,
                    isLoading = false,
                    isRefreshing = if (firstEmission) false else it.isRefreshing,
                    lastPostId = newPosts.lastOrNull()?.id
                ) }
                firstEmission = false
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val lastId = state.lastPostId ?: return
        if (state.isRefreshing) return
        if (state.currentTab == CommunityTab.DISCOVER) return // Paging 3 manages discovery page loading

        viewModelScope.launch {
            val tab = state.currentTab
            val uid = state.userId

            val flow = when (tab) {
                CommunityTab.DISCOVER -> flowOf(emptyList())
                CommunityTab.FOLLOWING -> if (uid.isNotEmpty()) feedRepository.getFollowingFeed(uid, lastId) else flowOf(emptyList())
                CommunityTab.SAVED -> flowOf(emptyList())
                CommunityTab.NEARBY -> flowOf(emptyList())
            }

            flow.take(1).collect { morePosts ->
                if (morePosts.isNotEmpty()) {
                    _uiState.update { currentState ->
                        val updatedPosts = (currentState.posts + morePosts).distinctBy { it.id }
                        currentState.copy(
                            posts = updatedPosts,
                            lastPostId = morePosts.lastOrNull()?.id
                        )
                    }
                }
            }
        }
    }

    fun refresh(clearPosts: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            if (_uiState.value.currentTab == CommunityTab.DISCOVER) {
                updateLastSeenTimestamp()
                _forceRefreshSharedFlow.emit(true)
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
            } else {
                if (clearPosts) {
                    _uiState.update { it.copy(posts = emptyList(), isLoading = true) }
                }
                _uiState.update { it.copy(lastPostId = null) }
                startFeedCollection()
            }
        }
    }

    fun forceExpireCache() {
        viewModelScope.launch {
            feedRepository.forceExpireCache()
        }
    }

    fun setTab(tab: CommunityTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setUserId(id: String) {
        _uiState.update { it.copy(userId = id) }
    }

    fun toggleSave(postId: String) {
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) post.copy(isSavedByMe = !post.isSavedByMe)
                else post
            }
            state.copy(posts = updatedPosts)
        }
        viewModelScope.launch {
            feedRepository.toggleSave(_uiState.value.userId, postId)
        }
    }

    fun toggleVote(postId: String, voteValue: Int) {
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val currentVote = post.myVoteValue
                    if (currentVote == voteValue) {
                        val scoreDiff = -voteValue
                        val newTruth = if (voteValue > 0) post.truthCount - 1 else post.truthCount
                        val newFalse = if (voteValue < 0) post.falseCount - 1 else post.falseCount
                        post.copy(
                            myVoteValue = 0,
                            votesScore = post.votesScore + scoreDiff,
                            truthCount = newTruth,
                            falseCount = newFalse
                        )
                    } else {
                        val scoreDiff = if (currentVote == 0) voteValue else voteValue - currentVote
                        val newTruth = post.truthCount + (if (voteValue > 0) 1 else 0) - (if (currentVote > 0) 1 else 0)
                        val newFalse = post.falseCount + (if (voteValue < 0) 1 else 0) - (if (currentVote < 0) 1 else 0)
                        post.copy(
                            myVoteValue = voteValue,
                            votesScore = post.votesScore + scoreDiff,
                            truthCount = newTruth,
                            falseCount = newFalse
                        )
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

    fun deletePost(postId: String, photoUrls: List<String>) {
        _uiState.update { state -> state.copy(posts = state.posts.filter { it.id != postId }) }
        viewModelScope.launch {
            feedRepository.deletePost(postId, photoUrls).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Publicación eliminada") }
            }
        }
    }

    fun markAsExpired(postId: String) {
        _uiState.update { state -> 
            state.copy(posts = state.posts.map { 
                if (it.id == postId) it.copy(verificationStatus = com.farbalapps.rinde.domain.model.VerificationStatus.EXPIRED) else it 
            }) 
        }
        viewModelScope.launch {
            feedRepository.markPostAsExpired(postId)
        }
    }

    fun reportAsExpired(postId: String, postTitle: String, authorId: String) {
        viewModelScope.launch {
            feedRepository.reportPostAsExpired(
                postId = postId,
                postTitle = postTitle,
                authorId = authorId,
                currentUserId = _uiState.value.userId,
                currentUserName = _uiState.value.userName
            ).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Reporte enviado al autor") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
