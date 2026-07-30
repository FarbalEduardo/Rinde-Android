package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE key = :key LIMIT 1")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE key = :key LIMIT 1")
    fun getMetadataSync(key: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll()
}
