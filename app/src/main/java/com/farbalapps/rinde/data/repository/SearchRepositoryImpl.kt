package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.SearchResult
import com.farbalapps.rinde.domain.repository.SearchRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val firestore: FirebaseFirestore
) : SearchRepository {

    companion object {
        private const val LOCAL_RESULT_THRESHOLD = 5
        private const val SEARCH_CACHE_TTL_MS = 5 * 60 * 1000L // 5 min TTL por query
        private const val FIRESTORE_LIMIT = 20L
    }

    override fun searchPosts(query: String, categoryFilter: String): Flow<SearchResult> = flow {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            emit(SearchResult.Idle)
            return@flow
        }

        emit(SearchResult.Loading)

        val localList = searchLocalDatabase(cleanQuery)
        val domainPosts = filterByCategory(localList.map { it.toDomainModel() }, categoryFilter)

        emit(SearchResult.Success(domainPosts))

        val shouldFetchRemote = domainPosts.size < LOCAL_RESULT_THRESHOLD && shouldFetchFromFirestore(cleanQuery)
        if (!shouldFetchRemote) {
            return@flow
        }

        try {
            val distinctRemoteDtos = fetchFromFirestore(cleanQuery)

            val remoteDomainPosts = distinctRemoteDtos.mapNotNull { it.toDomain() }
            if (remoteDomainPosts.isNotEmpty()) {
                postDao.upsertPosts(remoteDomainPosts.map { it.toEntity() })
            }
            saveSearchTtl(cleanQuery)

            val updatedLocalList = searchLocalDatabase(cleanQuery)
            val updatedDomain = filterByCategory(updatedLocalList.map { it.toDomainModel() }, categoryFilter)

            emit(SearchResult.Success(updatedDomain))

        } catch (e: Exception) {
            android.util.Log.e("SearchRepositoryImpl", "Error querying remote Firestore: ${e.message}")
        }
    }

    private suspend fun searchLocalDatabase(cleanQuery: String): List<com.farbalapps.rinde.data.local.entity.CommunityPostEntity> {
        val ftsFormattedQuery = "${cleanQuery.replace("\"", "").replace("'", "")}*"
        var localList = try {
            postDao.searchPostsFts(ftsFormattedQuery, 50)
        } catch (e: Exception) {
            emptyList()
        }

        if (localList.isEmpty()) {
            try {
                localList = postDao.searchPostsLike(cleanQuery, 50)
            } catch (e: Exception) {
                // Ignore fallback error
            }
        }
        return localList
    }

    private fun filterByCategory(posts: List<com.farbalapps.rinde.domain.model.CommunityPost>, categoryFilter: String): List<com.farbalapps.rinde.domain.model.CommunityPost> {
        if (categoryFilter.isBlank()) return posts
        
        return posts.filter { post ->
            val matchesCategory = post.category.equals(categoryFilter, ignoreCase = true)
            val matchesOfferType = when (categoryFilter.uppercase()) {
                "ONLINE", "EN LÍNEA", "EN LINEA" -> post.offerType == OfferType.ONLINE
                "PHYSICAL", "FÍSICO", "FISICO", "FÍSICA", "FISICA" -> post.offerType == OfferType.PHYSICAL
                else -> false
            }
            matchesCategory || matchesOfferType
        }
    }

    private suspend fun fetchFromFirestore(cleanQuery: String): List<CommunityPostDto> {
        val remoteDtos = mutableListOf<CommunityPostDto>()
        
        val byCategory = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", cleanQuery)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(FIRESTORE_LIMIT)
            .get()
            .await()
        remoteDtos.addAll(byCategory.documents.mapNotNull { doc -> doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id) })

        val capitalizedQuery = cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        if (remoteDtos.size < LOCAL_RESULT_THRESHOLD) {
            val byStore = firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .orderBy("storeName")
                .whereGreaterThanOrEqualTo("storeName", capitalizedQuery)
                .whereLessThanOrEqualTo("storeName", capitalizedQuery + "\uf8ff")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(FIRESTORE_LIMIT)
                .get()
                .await()
            remoteDtos.addAll(byStore.documents.mapNotNull { doc -> doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id) })
        }

        if (remoteDtos.size < LOCAL_RESULT_THRESHOLD) {
            val byTitle = firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .orderBy("title")
                .whereGreaterThanOrEqualTo("title", capitalizedQuery)
                .whereLessThanOrEqualTo("title", capitalizedQuery + "\uf8ff")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(FIRESTORE_LIMIT)
                .get()
                .await()
            remoteDtos.addAll(byTitle.documents.mapNotNull { doc -> doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id) })
        }

        return remoteDtos.distinctBy { it.id }
    }

    private suspend fun shouldFetchFromFirestore(query: String): Boolean {
        val ttlKey = "search_${query.lowercase().hashCode()}"
        val meta = syncMetadataDao.getMetadata(ttlKey) ?: return true
        return (System.currentTimeMillis() - meta.lastSyncTimestamp) > SEARCH_CACHE_TTL_MS
    }

    private suspend fun saveSearchTtl(query: String) {
        val ttlKey = "search_${query.lowercase().hashCode()}"
        val now = System.currentTimeMillis()
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = ttlKey,
                lastSyncTimestamp = now,
                lastSeenTimestamp = now
            )
        )
    }
}
