package com.farbalapps.rinde.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history_prefs")

@Singleton
class SearchHistoryDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private const val MAX_HISTORY = 5
    }

    val recentSearches: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        val raw = prefs[RECENT_SEARCHES_KEY] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("|||")
    }

    suspend fun addSearch(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        context.searchHistoryDataStore.edit { prefs ->
            val current = (prefs[RECENT_SEARCHES_KEY] ?: "")
                .split("|||")
                .filter { it.isNotBlank() && !it.equals(cleanQuery, ignoreCase = true) }
                .toMutableList()
            
            current.add(0, cleanQuery)
            val updated = current.take(MAX_HISTORY).joinToString("|||")
            prefs[RECENT_SEARCHES_KEY] = updated
        }
    }

    suspend fun removeSearch(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val current = (prefs[RECENT_SEARCHES_KEY] ?: "")
                .split("|||")
                .filter { it.isNotBlank() && !it.equals(query.trim(), ignoreCase = true) }
            prefs[RECENT_SEARCHES_KEY] = current.joinToString("|||")
        }
    }

    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(RECENT_SEARCHES_KEY)
        }
    }
}
