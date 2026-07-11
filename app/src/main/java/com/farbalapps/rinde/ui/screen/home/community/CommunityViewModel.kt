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
    DISCOVER, HOT, SAVED
}

data class CommunityUiState(
    val posts: List<CommunityPost> = emptyList(),
    val currentTab: CommunityTab = CommunityTab.DISCOVER,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isSavedLoading: Boolean = true,
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

    val postStatusOverlay: StateFlow<Map<String, com.farbalapps.rinde.domain.model.VerificationStatus>> = feedRepository.globalPostStatus

    val savedOverlay: StateFlow<Map<String, Boolean>> = feedRepository.globalSavedStatus

    val voteOverlay: StateFlow<Map<String, com.farbalapps.rinde.domain.repository.VoteOverlay>> = feedRepository.globalVoteStatus

    private val _userId = authRepository.getCurrentUser()?.id ?: ""

    private val _forceRefreshSharedFlow = MutableSharedFlow<Boolean>(replay = 1)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedFeed: Flow<PagingData<CommunityPost>> = _forceRefreshSharedFlow
        .onStart { emit(false) }
        .flatMapLatest { force ->
            feedRepository.getPagedFeed(forceRefresh = force)
        }
        .cachedIn(viewModelScope)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val hotPagedFeed: Flow<PagingData<CommunityPost>> = _forceRefreshSharedFlow
        .onStart { emit(false) }
        .flatMapLatest { force ->
            feedRepository.getHotPagedFeed(forceRefresh = force)
        }
        .cachedIn(viewModelScope)

    private var feedJob: Job? = null

    init {
        val user = authRepository.getCurrentUser()
        _uiState.update { it.copy(userId = user?.id ?: "", userName = user?.displayName ?: "") }
        
        // Sync user votes and saved posts early so we have them loaded
        if (_userId.isNotEmpty()) {
            viewModelScope.launch {
                feedRepository.syncUserVotes(_userId)
                feedRepository.syncUserSavedPosts(_userId)
            }
        }

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
            if (!hasCachedData) {
                _forceRefreshSharedFlow.emit(true)
            }
        }

        viewModelScope.launch {
            _uiState.map { it.currentTab }.distinctUntilChanged().collectLatest { tab ->
                _uiState.update { it.copy(newPostsCount = 0) }
                if (tab == CommunityTab.DISCOVER || tab == CommunityTab.HOT) {
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

        // Polling loop cada 10 minutos para VERIFICAR si hay nuevas publicaciones en Discover
        viewModelScope.launch {
            while (true) {
                delay(10 * 60 * 1000L)
                if (_uiState.value.currentTab == CommunityTab.DISCOVER) {
                    checkForNewPostsOnResume()
                }
            }
        }
    }

    fun onFirstLoadComplete() {
        // No-op or deleted since we removed isFirstLoad
    }

    fun showPendingPosts() {
        _uiState.update { it.copy(newPostsCount = 0) }
        viewModelScope.launch {
            feedRepository.refreshFeedIfNeeded(forceRefresh = true)
            updateLastSeenTimestamp()
            _forceRefreshSharedFlow.emit(true)
        }
    }

    private suspend fun updateLastSeenTimestamp() {
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
            // Si es la primera vez (lastSeen = 0), usamos la última hora para evitar falsos negativos
            val lastSeen = meta?.lastSeenTimestamp ?: meta?.lastSyncTimestamp ?: (System.currentTimeMillis() - 60 * 60 * 1000L)
            
            val count = feedRepository.countNewPostsSince(lastSeen)
            if (count >= 1) {
                _uiState.update { it.copy(newPostsCount = count) }
            } else {
                _uiState.update { it.copy(newPostsCount = 0) }
            }
        }
    }

    private fun startFeedCollection() {
        feedJob?.cancel()
        _uiState.update { it.copy(newPostsCount = 0) }

        val tab = _uiState.value.currentTab
        if (tab == CommunityTab.DISCOVER || tab == CommunityTab.HOT) {
            return // Managed by Paging 3 flow
        }

        feedJob = viewModelScope.launch {
            val uid = _uiState.value.userId

            if (uid.isNotEmpty()) {
                feedRepository.syncUserSavedPosts(uid)
            }

            val flow = when (tab) {
                CommunityTab.SAVED -> if (uid.isNotEmpty()) feedRepository.getSavedPosts(uid) else flowOf(emptyList())
                else -> flowOf(emptyList())
            }

            var firstEmission = true
            flow.collectLatest { newPosts ->
                _uiState.update { it.copy(
                    posts = newPosts,
                    isLoading = false,
                    isSavedLoading = false,
                    isRefreshing = if (firstEmission) false else it.isRefreshing,
                    lastPostId = newPosts.lastOrNull()?.id
                ) }
                firstEmission = false
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isRefreshing) return
        if (state.currentTab == CommunityTab.DISCOVER || state.currentTab == CommunityTab.HOT) return // Paging 3 manages discovery/hot page loading
    }

    fun refresh(clearPosts: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val tab = _uiState.value.currentTab
            if (tab == CommunityTab.DISCOVER || tab == CommunityTab.HOT) {
                if (tab == CommunityTab.DISCOVER) {
                    updateLastSeenTimestamp()
                }
                _forceRefreshSharedFlow.emit(true)
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
            } else {
                if (clearPosts) {
                    _uiState.update { it.copy(posts = emptyList(), isLoading = true, isSavedLoading = true) }
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
        viewModelScope.launch {
            feedRepository.toggleSave(_uiState.value.userId, postId)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(snackbarMessage = "No se pudo guardar la publicación. Intenta de nuevo.")
                    }
                }
        }
    }

    fun toggleVote(postId: String, voteValue: Int) {
        // 1. Actualización optimista de posts en UI (para pestaña de guardados) sin tocar contadores
        var originalVote = 0
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    originalVote = post.myVoteValue
                    val nextVote = if (originalVote == voteValue) 0 else voteValue
                    post.copy(myVoteValue = nextVote)
                } else post
            }
            state.copy(posts = updatedPosts)
        }

        viewModelScope.launch {
            // 2. Obtener authorId de la lista de posts en memoria o desde Room (para Discover/Hot)
            val authorId = _uiState.value.posts.find { it.id == postId }?.authorId 
                ?: postDao.getPostById(postId)?.authorId 
                ?: return@launch

            val result = toggleVoteUseCase(postId, voteValue, authorId)
            if (result.isFailure) {
                // 3. Revertir cambio optimista y mostrar error en caso de fallo
                _uiState.update { state ->
                    val revertedPosts = state.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(myVoteValue = originalVote)
                        } else post
                    }
                    state.copy(
                        posts = revertedPosts,
                        snackbarMessage = "No se pudo registrar tu voto. Revisa tu conexión."
                    )
                }
            }
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
