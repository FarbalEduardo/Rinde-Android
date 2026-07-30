package com.farbalapps.rinde.domain.model

sealed class SearchResult {
    object Idle : SearchResult()
    object Loading : SearchResult()
    data class Success(val posts: List<CommunityPost>) : SearchResult()
    data class Error(val message: String) : SearchResult()
}
