package com.farbalapps.rinde.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.RemoteMediator
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class HotPostRemoteMediatorTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var postDao: PostDao
    private lateinit var syncMetadataDao: SyncMetadataDao

    @Before
    fun setUp() {
        firestore = mockk()
        postDao = mockk()
        syncMetadataDao = mockk()
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun initialize_whenCacheIsValidAndHasLocalPosts_returnsSkipInitialRefresh() = runBlocking {
        val now = System.currentTimeMillis()
        val meta = SyncMetadataEntity(key = "feed_hot", lastSyncTimestamp = now - 5 * 60 * 1000L) // 5 minutes (TTL is 30 mins)

        coEvery { syncMetadataDao.getMetadata("feed_hot") } returns meta
        coEvery { postDao.getHotPostsCount() } returns 5

        val mediator = HotPostRemoteMediator(
            firestore = firestore,
            postDao = postDao,
            syncMetadataDao = syncMetadataDao,
            forceRefresh = false
        )

        val result = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, result)
    }

    @Test
    fun initialize_whenCacheIsExpired_returnsLaunchInitialRefresh() = runBlocking {
        val now = System.currentTimeMillis()
        val meta = SyncMetadataEntity(key = "feed_hot", lastSyncTimestamp = now - 45 * 60 * 1000L) // 45 minutes (TTL is 30 mins)

        coEvery { syncMetadataDao.getMetadata("feed_hot") } returns meta
        coEvery { postDao.getHotPostsCount() } returns 5

        val mediator = HotPostRemoteMediator(
            firestore = firestore,
            postDao = postDao,
            syncMetadataDao = syncMetadataDao,
            forceRefresh = false
        )

        val result = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, result)
    }

    @Test
    fun initialize_whenForceRefreshIsTrue_returnsLaunchInitialRefresh() = runBlocking {
        val now = System.currentTimeMillis()
        val meta = SyncMetadataEntity(key = "feed_hot", lastSyncTimestamp = now - 5 * 60 * 1000L) // Cache technically valid

        coEvery { syncMetadataDao.getMetadata("feed_hot") } returns meta
        coEvery { postDao.getHotPostsCount() } returns 5

        val mediator = HotPostRemoteMediator(
            firestore = firestore,
            postDao = postDao,
            syncMetadataDao = syncMetadataDao,
            forceRefresh = true
        )

        val result = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, result)
    }
}
