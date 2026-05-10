package com.farbalapps.rinde.ui.screen.home.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CommunityTab {
    RECENT, FOLLOWING, SAVED
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(CommunityTab.RECENT)
    val currentTab: StateFlow<CommunityTab> = _currentTab

    private val _userId = MutableStateFlow(authRepository.getCurrentUser()?.id ?: "")

    private val _lastPostId = MutableStateFlow<String?>(null)
    private val _allPosts = MutableStateFlow<List<CommunityPost>>(emptyList())
    
    val posts: StateFlow<List<CommunityPost>> = _allPosts.asStateFlow()

    init {
        // Automatically load initial feed and handle tab changes
        viewModelScope.launch {
            _currentTab.collectLatest { 
                _allPosts.value = emptyList()
                _lastPostId.value = null
                loadPage()
            }
        }
    }

    private suspend fun loadPage() {
        val uid = _userId.value
        val tab = _currentTab.value
        val lastId = _lastPostId.value

        val flow = when (tab) {
            CommunityTab.RECENT -> feedRepository.getGlobalFeed(lastId)
            CommunityTab.FOLLOWING -> if (uid.isNotEmpty()) feedRepository.getFollowingFeed(uid, lastId) else flowOf(emptyList())
            CommunityTab.SAVED -> if (uid.isNotEmpty()) feedRepository.getSavedPosts(uid) else flowOf(emptyList())
        }

        flow.take(1).collect { newPosts ->
            _allPosts.update { current -> (current + newPosts).distinctBy { it.id } }
            _lastPostId.value = newPosts.lastOrNull()?.id
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            loadPage()
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
}
