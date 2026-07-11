package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.repository.delegate.FeedInteractionDelegate
import com.farbalapps.rinde.data.repository.delegate.FeedLifecycleDelegate
import com.farbalapps.rinde.data.repository.delegate.FeedPaginationDelegate
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.repository.VoteOverlay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val paginationDelegate: FeedPaginationDelegate,
    private val interactionDelegate: FeedInteractionDelegate,
    private val lifecycleDelegate: FeedLifecycleDelegate
) : FeedRepository {

    override val globalPostStatus: StateFlow<Map<String, VerificationStatus>>
        get() = interactionDelegate.globalPostStatus

    override val globalSavedStatus: StateFlow<Map<String, Boolean>>
        get() = interactionDelegate.globalSavedStatus

    override val globalVoteStatus: StateFlow<Map<String, VoteOverlay>>
        get() = interactionDelegate.globalVoteStatus

    override fun updatePostStatusLocal(postId: String, status: VerificationStatus) {
        interactionDelegate.updatePostStatusLocal(postId, status)
    }

    override fun updateSavedStatusLocal(postId: String, isSaved: Boolean) {
        interactionDelegate.updateSavedStatusLocal(postId, isSaved)
    }

    override fun getPagedFeed(forceRefresh: Boolean): Flow<androidx.paging.PagingData<CommunityPost>> {
        return paginationDelegate.getPagedFeed(forceRefresh) { post ->
            lifecycleDelegate.enrichPost(post)
        }
    }

    override fun getHotPagedFeed(forceRefresh: Boolean): Flow<androidx.paging.PagingData<CommunityPost>> {
        return paginationDelegate.getHotPagedFeed(forceRefresh) { post ->
            lifecycleDelegate.enrichPost(post)
        }
    }

    override fun getNearbyFeed(lat: Double, lon: Double, radiusKm: Double): Flow<List<CommunityPost>> {
        return paginationDelegate.getNearbyFeed(lat, lon, radiusKm) { snapshot ->
            lifecycleDelegate.snapshotToPosts(snapshot)
        }
    }

    override fun getSmartInterestFeed(interests: List<String>, lastPostId: String?): Flow<List<CommunityPost>> {
        return paginationDelegate.getSmartInterestFeed(interests) { snapshot ->
            lifecycleDelegate.snapshotToPosts(snapshot)
        }
    }

    override fun getSavedPosts(userId: String): Flow<List<CommunityPost>> {
        return interactionDelegate.getSavedPosts(userId) { snapshot ->
            lifecycleDelegate.snapshotToPosts(snapshot)
        }
    }

    override fun getPostById(postId: String): Flow<CommunityPost> =
        lifecycleDelegate.getPostById(postId)

    override fun getUserPosts(userId: String): Flow<List<CommunityPost>> =
        lifecycleDelegate.getUserPosts(userId)

    override suspend fun countNewPostsSince(sinceTimestamp: Long): Int =
        lifecycleDelegate.countNewPostsSince(sinceTimestamp)

    override suspend fun refreshFeedIfNeeded(forceRefresh: Boolean): Result<Int> =
        lifecycleDelegate.refreshFeedIfNeeded(forceRefresh)

    override suspend fun forceExpireCache() =
        lifecycleDelegate.forceExpireCache()

    override suspend fun syncUserVotes(userId: String): Result<Unit> =
        lifecycleDelegate.syncUserVotes(userId)

    override suspend fun syncUserSavedPosts(userId: String): Result<Unit> =
        lifecycleDelegate.syncUserSavedPosts(userId)

    override suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit> =
        lifecycleDelegate.uploadPost(post, photoUris)

    override suspend fun deletePost(postId: String, photoUrls: List<String>): Result<Unit> =
        lifecycleDelegate.deletePost(postId, photoUrls)

    override suspend fun updatePost(
        postId: String, title: String, descriptionLong: String, category: String,
        normalPrice: Double?, discountPrice: Double?, currency: String,
        couponCode: String?, discountPercentage: Int?, isAvailable: Boolean,
        condition: String, websiteName: String?, productLink: String?,
        storeName: String?, locationName: String, oldPhotoUrls: List<String>,
        newPhotoUris: List<String>
    ): Result<Unit> =
        lifecycleDelegate.updatePost(
            postId, title, descriptionLong, category, normalPrice, discountPrice,
            currency, couponCode, discountPercentage, isAvailable, condition,
            websiteName, productLink, storeName, locationName, oldPhotoUrls, newPhotoUris
        )

    override suspend fun markPostAsExpired(postId: String): Result<Unit> =
        lifecycleDelegate.markPostAsExpired(postId)

    override suspend fun markPostAsAvailable(postId: String): Result<Unit> =
        lifecycleDelegate.markPostAsAvailable(postId)

    override suspend fun reportPostAsExpired(
        postId: String, postTitle: String, authorId: String,
        currentUserId: String, currentUserName: String
    ): Result<Unit> =
        lifecycleDelegate.reportPostAsExpired(postId, postTitle, authorId, currentUserId, currentUserName)

    override fun getUnreadNotificationsCount(userId: String): Flow<Int> =
        // Reutilizado el endpoint en lifecycle (o se puede mover a un delegate específico de notificaciones si crece)
        callbackFlow {
            val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    trySend(snapshot?.size() ?: 0)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun toggleLike(userId: String, postId: String): Result<Unit> =
        interactionDelegate.toggleLike(userId, postId)

    override suspend fun toggleSave(userId: String, postId: String): Result<Unit> =
        interactionDelegate.toggleSave(userId, postId)

    override suspend fun toggleVote(userId: String, postId: String, voteValue: Int): Result<Unit> =
        interactionDelegate.toggleVote(userId, postId, voteValue)

    override fun clearSessionState() =
        interactionDelegate.clearSessionState()
}
