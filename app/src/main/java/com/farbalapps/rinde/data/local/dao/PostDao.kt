package com.farbalapps.rinde.data.local.dao

import androidx.room.*
import com.farbalapps.rinde.data.local.entity.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM community_posts WHERE isActive = 1 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPosts(limit: Int = 20, offset: Int = 0): Flow<List<CommunityPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CommunityPostEntity>)

    @Query("SELECT MAX(timestamp) FROM community_posts")
    suspend fun getLatestTimestamp(): Long?

    @Query("DELETE FROM community_posts WHERE timestamp < :threshold")
    suspend fun deleteOldPosts(threshold: Long)

    @Query("DELETE FROM community_posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM community_posts")
    suspend fun clearAll()
}
