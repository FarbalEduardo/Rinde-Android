package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.CommunityPost
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getPagedFeed(forceRefresh: Boolean = false): Flow<androidx.paging.PagingData<CommunityPost>>
    fun getFollowingFeed(userId: String, lastPostId: String? = null): Flow<List<CommunityPost>>
    fun getSavedPosts(userId: String): Flow<List<CommunityPost>>
    fun getNearbyFeed(lat: Double, lon: Double, radiusKm: Double): Flow<List<CommunityPost>>
    fun getSmartInterestFeed(interests: List<String>, lastPostId: String? = null): Flow<List<CommunityPost>>

    /**
     * Observa un único post en tiempo real por su ID.
     * Se usa en la pantalla de detalle para mantener los datos actualizados.
     */
    fun getPostById(postId: String): Flow<CommunityPost>

    /**
     * NUEVO: Posts de un usuario (todos los estados) — para Perfil
     */
    fun getUserPosts(userId: String): Flow<List<CommunityPost>>

    /**
     * NUEVO: Conteo de nuevas publicaciones desde sinceTimestamp usando count() aggregation (sin documentos).
     */
    suspend fun countNewPostsSince(sinceTimestamp: Long): Int

    /**
     * NUEVO: Refresca el feed si el TTL expiró o si es un refresco forzado.
     */
    suspend fun refreshFeedIfNeeded(forceRefresh: Boolean = false): Result<Int>

    suspend fun forceExpireCache()

    suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit>

    suspend fun toggleLike(userId: String, postId: String): Result<Unit>
    suspend fun toggleSave(userId: String, postId: String): Result<Unit>
    suspend fun toggleVote(userId: String, postId: String, voteValue: Int): Result<Unit>
    suspend fun syncUserVotes(userId: String): Result<Unit>
    suspend fun syncUserSavedPosts(userId: String): Result<Unit>

    suspend fun deletePost(postId: String, photoUrls: List<String>): Result<Unit>
    suspend fun updatePost(
        postId: String,
        title: String,
        descriptionLong: String,
        category: String,
        normalPrice: Double?,
        discountPrice: Double?,
        currency: String,
        couponCode: String?,
        discountPercentage: Int?,
        isAvailable: Boolean,
        condition: String,
        websiteName: String?,
        productLink: String?,
        storeName: String?,
        locationName: String,
        oldPhotoUrls: List<String>,
        newPhotoUris: List<String>
    ): Result<Unit>
    suspend fun markPostAsExpired(postId: String): Result<Unit>
    suspend fun reportPostAsExpired(postId: String, postTitle: String, authorId: String, currentUserId: String, currentUserName: String): Result<Unit>
    fun getUnreadNotificationsCount(userId: String): Flow<Int>
}
