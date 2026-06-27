package com.farbalapps.rinde.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.CommunityPostEntity
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val forceRefresh: Boolean = false
) : RemoteMediator<Int, CommunityPostEntity>() {

    companion object {
        private const val FEED_KEY = "feed_global"
        private const val PAGE_SIZE = 20L
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes TTL
    }

    override suspend fun initialize(): InitializeAction {
        val meta = syncMetadataDao.getMetadata(FEED_KEY)
        val now = System.currentTimeMillis()
        val hasCachedData = postDao.getPostsOnce(1).isNotEmpty()
        val isWithinTtl = meta != null && (now - meta.lastSyncTimestamp) < CACHE_TTL_MS

        return if (!forceRefresh && hasCachedData && isWithinTtl) {
            android.util.Log.d("PostRemoteMediator", "Cache válido + datos locales. SKIP_INITIAL_REFRESH.")
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CommunityPostEntity>
    ): MediatorResult {
        android.util.Log.d("PostRemoteMediator", "load() called with LoadType: $loadType")
        return try {
            val result = when (loadType) {
                LoadType.REFRESH -> handleRefresh(state)
                LoadType.APPEND -> handleAppend(state)
                LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            }
            android.util.Log.d("PostRemoteMediator", "load() finished. LoadType: $loadType, Result: $result")
            result
        } catch (e: Exception) {
            android.util.Log.e("PostRemoteMediator", "Error loading data in Mediator: ${e.message}", e)
            MediatorResult.Error(e)
        }
    }

    private suspend fun handleRefresh(state: PagingState<Int, CommunityPostEntity>): MediatorResult {
        val meta = syncMetadataDao.getMetadata(FEED_KEY)
        val now = System.currentTimeMillis()
        val sinceTimestamp = meta?.lastSyncTimestamp ?: 0L

        // TTL cache validation
        if (!forceRefresh && meta != null && (now - meta.lastSyncTimestamp) < CACHE_TTL_MS) {
            android.util.Log.d("PostRemoteMediator", "TTL is still valid. Serving from Room cache.")
            return MediatorResult.Success(endOfPaginationReached = false)
        }

        android.util.Log.d("PostRemoteMediator", "TTL expired or forceRefresh active. Fetching incremental posts from Firestore.")
        val isFirstLoad = sinceTimestamp == 0L
        android.util.Log.d("PostRemoteMediator", "handleRefresh parameters: sinceTimestamp=$sinceTimestamp, isFirstLoad=$isFirstLoad, forceRefresh=$forceRefresh")
        
        val query = if (isFirstLoad) {
            firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
        } else {
            firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .whereGreaterThan("timestamp", Date(sinceTimestamp))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
        }
        
        val snapshot = query.get().await()
        android.util.Log.d("PostRemoteMediator", "Firestore query returned ${snapshot.size()} documents.")

        val posts = snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
            } catch (ex: Exception) {
                android.util.Log.e("PostRemoteMediator", "Error parsing document ${doc.id}: ${ex.message}", ex)
                null
            }
        }
        android.util.Log.d("PostRemoteMediator", "Successfully parsed ${posts.size} posts.")

        if (posts.isNotEmpty()) {
            postDao.upsertPosts(posts.map { it.toEntity() })
            android.util.Log.d("PostRemoteMediator", "Inserted ${posts.size} posts into Room.")
        }

        // Save metadata sync
        val newMeta = (meta ?: SyncMetadataEntity(key = FEED_KEY)).copy(
            lastSyncTimestamp = now
        )
        syncMetadataDao.upsert(newMeta)

        return MediatorResult.Success(endOfPaginationReached = posts.size < PAGE_SIZE)
    }

    private suspend fun handleAppend(state: PagingState<Int, CommunityPostEntity>): MediatorResult {
        val lastItem = state.lastItemOrNull() ?: return MediatorResult.Success(endOfPaginationReached = true)
        
        android.util.Log.d("PostRemoteMediator", "Appending page after timestamp: ${lastItem.timestamp}")
        val snapshot = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereLessThan("timestamp", Date(lastItem.timestamp))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .get()
            .await()

        val posts = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
        }

        if (posts.isNotEmpty()) {
            postDao.upsertPosts(posts.map { it.toEntity() })
        }

        return MediatorResult.Success(endOfPaginationReached = posts.size < PAGE_SIZE)
    }
}
