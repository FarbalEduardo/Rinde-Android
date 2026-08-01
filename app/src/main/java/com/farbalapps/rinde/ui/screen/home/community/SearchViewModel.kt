package com.farbalapps.rinde.ui.screen.home.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.data.local.SearchHistoryDataStore
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.SearchResult
import com.farbalapps.rinde.domain.usecase.SearchPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedCategory: String = "",
    val posts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val searchHistoryDataStore: SearchHistoryDataStore
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Observar historial de búsquedas recientes desde DataStore
        viewModelScope.launch {
            searchHistoryDataStore.recentSearches.collect { history ->
                _uiState.update { it.copy(recentSearches = history) }
            }
        }

        // Búsqueda reactiva con debounce de 400ms
        viewModelScope.launch {
            combine(_query, _selectedCategory) { q, cat -> Pair(q, cat) }
                .debounce(400L)
                .flatMapLatest { (q, cat) ->
                    if (q.trim().length < 2) {
                        flowOf(SearchResult.Idle)
                    } else {
                        searchPostsUseCase(q, cat)
                    }
                }
                .collect { result ->
                    _uiState.update { state ->
                        when (result) {
                            is SearchResult.Idle -> state.copy(
                                posts = emptyList(),
                                isLoading = false,
                                error = null
                            )
                            is SearchResult.Loading -> state.copy(
                                isLoading = true,
                                error = null
                            )
                            is SearchResult.Success -> state.copy(
                                posts = result.posts,
                                isLoading = false,
                                error = null
                            )
                            is SearchResult.Error -> state.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _uiState.update { it.copy(query = newQuery) }
    }

    fun onCategorySelect(category: String) {
        val next = if (_selectedCategory.value == category) "" else category
        _selectedCategory.value = next
        _uiState.update { it.copy(selectedCategory = next) }
    }

    fun onSearchTriggered(queryToSave: String) {
        if (queryToSave.trim().length >= 2) {
            viewModelScope.launch {
                searchHistoryDataStore.addSearch(queryToSave)
            }
        }
    }

    fun removeRecentSearch(search: String) {
        viewModelScope.launch {
            searchHistoryDataStore.removeSearch(search)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchHistoryDataStore.clearHistory()
        }
    }
}
