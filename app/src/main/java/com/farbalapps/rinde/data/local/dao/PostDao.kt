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

    @Query("SELECT * FROM community_posts WHERE isActive = 1 AND votesScore >= 50 ORDER BY votesScore DESC, timestamp DESC")
    fun getHotPostsPagingSource(): androidx.paging.PagingSource<Int, CommunityPostEntity>

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

    /** Actualiza SOLO el campo isSavedByMe sin tocar el resto de la fila (evita race conditions con isActive). */
    @Query("UPDATE community_posts SET isSavedByMe = :isSaved WHERE id = :postId")
    suspend fun updateSavedStatus(postId: String, isSaved: Boolean)

    @Query("SELECT MAX(timestamp) FROM community_posts")
    suspend fun getMaxTimestamp(): Long?

    @Query("DELETE FROM community_posts")
    suspend fun clearAll()

    /** Resetea estado de usuario en todos los posts al cambiar de sesión. */
    @Query("UPDATE community_posts SET isSavedByMe = 0, myVoteValue = 0")
    suspend fun resetUserStateOnAllPosts()

    @Query("UPDATE community_posts SET myVoteValue = :voteValue, truthCount = :truthCount, falseCount = :falseCount, votesScore = :score WHERE id = :postId")
    suspend fun updateVoteState(postId: String, voteValue: Int, truthCount: Int, falseCount: Int, score: Int)

    @Query("UPDATE community_posts SET myVoteValue = :voteValue WHERE id = :postId")
    suspend fun updateVoteValueOnly(postId: String, voteValue: Int)
}

