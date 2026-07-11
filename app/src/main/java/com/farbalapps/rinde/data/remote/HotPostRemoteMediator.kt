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
class HotPostRemoteMediator(
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val forceRefresh: Boolean = false
) : RemoteMediator<Int, CommunityPostEntity>() {

    companion object {
        private const val FEED_KEY = "feed_hot"
        private const val PAGE_SIZE = 30L
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes TTL
    }

    override suspend fun initialize(): InitializeAction {
        val meta = syncMetadataDao.getMetadata(FEED_KEY)
        val now = System.currentTimeMillis()
        val isWithinTtl = meta != null && (now - meta.lastSyncTimestamp) < CACHE_TTL_MS

        return if (!forceRefresh && isWithinTtl) {
            android.util.Log.d("HotPostRemoteMediator", "Hot cache valid. SKIP_INITIAL_REFRESH.")
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CommunityPostEntity>
    ): MediatorResult {
        android.util.Log.d("HotPostRemoteMediator", "load() called with LoadType: $loadType")
        return try {
            val result = when (loadType) {
                LoadType.REFRESH -> handleRefresh(state)
                LoadType.APPEND -> handleAppend(state)
                LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            }
            android.util.Log.d("HotPostRemoteMediator", "load() finished. LoadType: $loadType, Result: $result")
            result
        } catch (e: Exception) {
            android.util.Log.e("HotPostRemoteMediator", "Error loading hot data in Mediator: ${e.message}", e)
            MediatorResult.Error(e)
        }
    }

    private suspend fun handleRefresh(state: PagingState<Int, CommunityPostEntity>): MediatorResult {
        val meta = syncMetadataDao.getMetadata(FEED_KEY)
        val now = System.currentTimeMillis()

        if (!forceRefresh && meta != null && (now - meta.lastSyncTimestamp) < CACHE_TTL_MS) {
            android.util.Log.d("HotPostRemoteMediator", "TTL is still valid. Serving hot posts from Room.")
            return MediatorResult.Success(endOfPaginationReached = false)
        }

        android.util.Log.d("HotPostRemoteMediator", "Fetching hot posts from Firestore (votesScore >= 50).")
        val query = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereGreaterThanOrEqualTo("votesScore", 50)
            .orderBy("votesScore", Query.Direction.DESCENDING)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)

        val snapshot = query.get().await()
        val posts = snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
            } catch (ex: Exception) {
                null
            }
        }

        if (posts.isNotEmpty()) {
            postDao.upsertPosts(posts.map { it.toEntity() })
        }

        val newMeta = (meta ?: SyncMetadataEntity(key = FEED_KEY)).copy(
            lastSyncTimestamp = now
        )
        syncMetadataDao.upsert(newMeta)

        return MediatorResult.Success(endOfPaginationReached = posts.size < PAGE_SIZE)
    }

    private suspend fun handleAppend(state: PagingState<Int, CommunityPostEntity>): MediatorResult {
        val lastItem = state.lastItemOrNull() ?: return MediatorResult.Success(endOfPaginationReached = true)
        
        android.util.Log.d("HotPostRemoteMediator", "Appending hot posts after votesScore: ${lastItem.votesScore}")
        
        val lastDocRef = firestore.collection("posts").document(lastItem.id).get().await()
        if (!lastDocRef.exists()) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        val snapshot = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereGreaterThanOrEqualTo("votesScore", 50)
            .orderBy("votesScore", Query.Direction.DESCENDING)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastDocRef)
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
