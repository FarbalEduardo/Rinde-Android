package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.PendingVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingVoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vote: PendingVoteEntity)

    @Query("SELECT * FROM pending_votes")
    suspend fun getAll(): List<PendingVoteEntity>

    @Query("DELETE FROM pending_votes WHERE postId = :postId AND userId = :userId")
    suspend fun delete(postId: String, userId: String)

    @Query("SELECT * FROM pending_votes WHERE postId = :postId AND userId = :userId LIMIT 1")
    suspend fun getByPost(postId: String, userId: String): PendingVoteEntity?

    @Query("SELECT COUNT(*) FROM pending_votes")
    fun observePendingCount(): Flow<Int>
}
