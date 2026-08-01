package com.farbalapps.rinde.data.local.dao

import androidx.room.*
import com.farbalapps.rinde.data.local.entity.SavedListEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the saved_lists table.
 */
@Dao
interface SavedListDao {

    @Query("SELECT * FROM saved_lists WHERE userId = :userId ORDER BY savedAt DESC")
    fun getSavedListsByUser(userId: String): Flow<List<SavedListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: SavedListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lists: List<SavedListEntity>)

    @Query("DELETE FROM saved_lists WHERE id = :listId")
    suspend fun deleteById(listId: String)

    @Query("UPDATE saved_lists SET name = :newName, lastModifiedAt = :updatedAt WHERE id = :listId")
    suspend fun updateName(listId: String, newName: String, updatedAt: Long = System.currentTimeMillis())
}
