package com.farbalapps.rinde.ui.screen.home.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.farbalapps.rinde.util.LocationService

enum class CommunityTab {
    DISCOVER, FOLLOWING, SAVED, NEARBY
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val toggleVoteUseCase: ToggleVoteUseCase,
    private val locationService: LocationService
) : ViewModel() {

    private val _currentTab = MutableStateFlow(CommunityTab.DISCOVER)
    val currentTab: StateFlow<CommunityTab> = _currentTab

    private val _userId = MutableStateFlow(authRepository.getCurrentUser()?.id ?: "")

    private val _lastPostId = MutableStateFlow<String?>(null)
    private val _allPosts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _allPosts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var feedJob: Job? = null

    init {
        // Automatically load initial feed and handle tab changes
        viewModelScope.launch {
            _currentTab.collectLatest { tab ->
                refresh()
            }
        }
    }

    private fun startFeedCollection() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            val uid = _userId.value
            val tab = _currentTab.value

            val flow = when (tab) {
                CommunityTab.DISCOVER -> feedRepository.getGlobalFeed(null)
                CommunityTab.FOLLOWING -> if (uid.isNotEmpty()) feedRepository.getFollowingFeed(uid, null) else flowOf(emptyList())
                CommunityTab.SAVED -> if (uid.isNotEmpty()) feedRepository.getSavedPosts(uid) else flowOf(emptyList())
                CommunityTab.NEARBY -> {
                    val location = locationService.getCurrentLocation()
                    if (location != null) {
                        feedRepository.getNearbyFeed(location.latitude, location.longitude, 10.0) // 10km radius
                    } else {
                        flowOf(emptyList())
                    }
                }
            }

            // Real-time updates for the first page
            flow.collectLatest { newPosts ->
                _allPosts.update { newPosts }
                _lastPostId.value = newPosts.lastOrNull()?.id
            }
        }
    }

    fun loadMore() {
        val lastId = _lastPostId.value ?: return
        if (_isRefreshing.value) return

        viewModelScope.launch {
            val uid = _userId.value
            val tab = _currentTab.value

            val flow = when (tab) {
                CommunityTab.DISCOVER -> feedRepository.getGlobalFeed(lastId)
                CommunityTab.FOLLOWING -> if (uid.isNotEmpty()) feedRepository.getFollowingFeed(uid, lastId) else flowOf(emptyList())
                CommunityTab.SAVED -> flowOf(emptyList()) 
                CommunityTab.NEARBY -> flowOf(emptyList()) // Paginación por ubicación es compleja, dejar simple por ahora
            }

            // One-shot for pagination to avoid infinite listener loops
            flow.take(1).collect { morePosts ->
                if (morePosts.isNotEmpty()) {
                    _allPosts.update { current -> (current + morePosts).distinctBy { it.id } }
                    _lastPostId.value = morePosts.lastOrNull()?.id
                }
            }
        }
    }


    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _allPosts.value = emptyList()
            _lastPostId.value = null
            startFeedCollection()
            _isRefreshing.value = false
        }
    }

    fun setTab(tab: CommunityTab) {
        _currentTab.value = tab
    }
    
    fun setUserId(id: String) {
        _userId.value = id
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleLike(_userId.value, postId)
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleSave(_userId.value, postId)
        }
    }

    fun toggleVote(postId: String, voteValue: Int) {
        viewModelScope.launch {
            toggleVoteUseCase(postId, voteValue)
        }
    }

}
