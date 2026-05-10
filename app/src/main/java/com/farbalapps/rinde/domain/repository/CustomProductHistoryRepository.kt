package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.CustomProductHistory
import kotlinx.coroutines.flow.Flow

interface CustomProductHistoryRepository {
    fun getHistory(): Flow<List<CustomProductHistory>>
    suspend fun saveToHistory(name: String, category: String)
    suspend fun deleteFromHistory(name: String)
}
