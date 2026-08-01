package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun searchPosts(query: String, categoryFilter: String = ""): Flow<SearchResult>
}
