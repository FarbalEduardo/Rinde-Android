package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY orderIndex ASC, name ASC")
    fun getCategoriesByUser(userId: String): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE name = :name AND userId = :userId")
    suspend fun deleteByName(name: String, userId: String)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}
