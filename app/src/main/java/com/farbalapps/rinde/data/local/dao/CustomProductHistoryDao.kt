package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.CustomProductHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomProductHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomProductHistoryEntity)

    @Query("SELECT * FROM custom_product_history ORDER BY addedAt DESC")
    fun getHistory(): Flow<List<CustomProductHistoryEntity>>

    @Query("DELETE FROM custom_product_history WHERE name = :name")
    suspend fun delete(name: String)
}
