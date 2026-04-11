package com.farbalapps.rinde.data.local.dao

import androidx.room.*
import com.farbalapps.rinde.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the shopping_items table.
 */
@Dao
interface ShoppingItemDao {

    /**
     * Observes items belonging to a specific user.
     * Emits a new list automatically whenever the table changes.
     */
    @Query("SELECT * FROM shopping_items WHERE userId = :userId ORDER BY isCompleted ASC, name ASC")
    fun getItemsByUser(userId: String): Flow<List<ShoppingItemEntity>>

    /**
     * Inserts or replaces a shopping item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity)

    /**
     * Updates an existing shopping item.
     */
    @Update
    suspend fun update(item: ShoppingItemEntity)

    /**
     * Deletes a shopping item.
     */
    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    /**
     * Deletes all items belonging to a specific shopping group.
     */
    @Query("DELETE FROM shopping_items WHERE listGroup = :group")
    suspend fun deleteByGroup(group: String)

    /**
     * Inserts multiple shopping items (used for sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingItemEntity>)
}
