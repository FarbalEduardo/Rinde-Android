package com.farbalapps.rinde.data.local.dao

import androidx.room.*
import com.farbalapps.rinde.data.local.entity.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM community_posts WHERE isActive = 1 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPosts(limit: Int = 20, offset: Int = 0): Flow<List<CommunityPostEntity>>

    /** One-shot: devuelve el caché actual sin suscribir un Flow. Usado para mostrar caché instantáneo al abrir la app. */
    @Query("SELECT * FROM community_posts WHERE isActive = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getPostsOnce(limit: Int = 30): List<CommunityPostEntity>


    @Query("SELECT * FROM community_posts WHERE isActive = 1 AND category IN (:categories) ORDER BY votesScore DESC, timestamp DESC")
    fun getPostsByCategories(categories: List<String>): Flow<List<CommunityPostEntity>>

    /** Usado por getFollowingFeed: devuelve posts cacheados de una lista de autores seguidos. */
    @Query("SELECT * FROM community_posts WHERE isActive = 1 AND authorId IN (:authorIds) ORDER BY timestamp DESC LIMIT :limit")
    fun getPostsByAuthorIds(authorIds: List<String>, limit: Int = 50): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE isActive = 1 AND authorId IN (:authorIds) ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getPostsByAuthorIdsOnce(authorIds: List<String>, limit: Int = 30): List<CommunityPostEntity>

    @Query("SELECT * FROM community_posts WHERE id IN (:postIds)")
    suspend fun getPostsByIds(postIds: List<String>): List<CommunityPostEntity>

    @Query("SELECT COUNT(*) FROM community_posts WHERE isActive = 1 AND timestamp > :sinceTimestamp")
    suspend fun countPostsNewerThan(sinceTimestamp: Long): Int

    /** Usado por ProfileRepositoryImpl: devuelve posts de un autor específico, incluyendo myVoteValue. */
    @Query("SELECT * FROM community_posts WHERE isActive = 1 AND authorId = :authorId ORDER BY timestamp DESC")
    fun getPostsByAuthorId(authorId: String): Flow<List<CommunityPostEntity>>

    /** Lookup puntual de un post por su ID (útil para enriquecer datos). */
    @Query("SELECT * FROM community_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): CommunityPostEntity?

    /** Observa en tiempo real un único post por ID — usado en PostDetailScreen. */
    @Query("SELECT * FROM community_posts WHERE id = :postId LIMIT 1")
    fun getPostByIdFlow(postId: String): Flow<CommunityPostEntity?>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CommunityPostEntity>)

    @Query("SELECT MAX(timestamp) FROM community_posts")
    suspend fun getLatestTimestamp(): Long?

    @Query("DELETE FROM community_posts WHERE timestamp < :threshold")
    suspend fun deleteOldPosts(threshold: Long)

    @Query("DELETE FROM community_posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("UPDATE community_posts SET authorName = :authorName, authorPhotoUrl = :authorPhotoUrl WHERE authorId = :authorId")
    suspend fun updateAuthorProfile(authorId: String, authorName: String, authorPhotoUrl: String?)

    @Query("UPDATE community_posts SET verificationStatus = :status WHERE id = :postId")
    suspend fun updateVerificationStatus(postId: String, status: String)

    @Query("SELECT * FROM community_posts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getPostsPagingSource(): androidx.paging.PagingSource<Int, CommunityPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<CommunityPostEntity>)

    @Update
    suspend fun updatePost(post: CommunityPostEntity)

    @Query("UPDATE community_posts SET isActive = :isActive WHERE id = :postId")
    suspend fun updatePostStatus(postId: String, isActive: Boolean)

    @Query("SELECT MAX(timestamp) FROM community_posts")
    suspend fun getMaxTimestamp(): Long?

    @Query("DELETE FROM community_posts")
    suspend fun clearAll()
}

