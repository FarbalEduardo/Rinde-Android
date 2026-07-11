package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.UserVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserVoteDao {
    @Query("SELECT * FROM user_votes WHERE postId = :postId AND userId = :userId")
    fun observeVote(postId: String, userId: String): Flow<UserVoteEntity?>

    @Query("SELECT * FROM user_votes WHERE postId = :postId AND userId = :userId")
    suspend fun getVoteOnce(postId: String, userId: String): UserVoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVote(vote: UserVoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVotes(votes: List<UserVoteEntity>)

    @Query("DELETE FROM user_votes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteVote(postId: String, userId: String)

    @Query("SELECT * FROM user_votes WHERE userId = :userId AND postId IN (:postIds)")
    suspend fun getVotesForPosts(userId: String, postIds: List<String>): List<UserVoteEntity>

    @Query("SELECT * FROM user_votes WHERE userId = :userId")
    suspend fun getAllVotesForUser(userId: String): List<UserVoteEntity>

    @Query("DELETE FROM user_votes WHERE userId = :userId")
    suspend fun clearUserVotes(userId: String)
}
