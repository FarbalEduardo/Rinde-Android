package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.SearchResult
import com.farbalapps.rinde.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchPostsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    operator fun invoke(query: String, categoryFilter: String = ""): Flow<SearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            return flowOf(SearchResult.Idle)
        }
        return searchRepository.searchPosts(cleanQuery, categoryFilter)
    }
}
