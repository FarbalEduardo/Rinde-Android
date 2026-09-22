package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfile(id: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
    
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("UPDATE profiles SET postsCount = MAX(0, postsCount + :delta) WHERE id = :id")
    suspend fun updatePostsCount(id: String, delta: Int)

    @Query("UPDATE profiles SET commentsCount = MAX(0, commentsCount + :delta) WHERE id = :id")
    suspend fun updateCommentsCount(id: String, delta: Int)
}
