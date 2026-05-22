package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.CommunityPost
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getGlobalFeed(lastPostId: String? = null): Flow<List<CommunityPost>>
    fun getFollowingFeed(userId: String, lastPostId: String? = null): Flow<List<CommunityPost>>
    fun getSavedPosts(userId: String): Flow<List<CommunityPost>>
    fun getNearbyFeed(lat: Double, lon: Double, radiusKm: Double): Flow<List<CommunityPost>>
    suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit>
    suspend fun toggleLike(userId: String, postId: String): Result<Unit>
    suspend fun toggleSave(userId: String, postId: String): Result<Unit>
    suspend fun toggleVote(userId: String, postId: String, voteValue: Int): Result<Unit>
}
