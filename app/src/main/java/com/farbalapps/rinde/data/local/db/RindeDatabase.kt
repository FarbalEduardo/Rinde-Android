package com.farbalapps.rinde.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.farbalapps.rinde.data.local.dao.CategoryDao
import com.farbalapps.rinde.data.local.dao.CustomProductHistoryDao
import com.farbalapps.rinde.data.local.dao.ShoppingItemDao
import com.farbalapps.rinde.data.local.entity.CategoryEntity
import com.farbalapps.rinde.data.local.entity.CustomProductHistoryEntity
import com.farbalapps.rinde.data.local.entity.ShoppingItemEntity
import com.farbalapps.rinde.data.local.entity.ProfileEntity
import com.farbalapps.rinde.data.local.dao.ProfileDao

import com.farbalapps.rinde.data.local.entity.CommunityPostEntity
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.UserVoteEntity
import com.farbalapps.rinde.data.local.dao.UserVoteDao
import com.farbalapps.rinde.data.local.entity.SavingsGoalEntity
import com.farbalapps.rinde.data.local.entity.GoalTransactionEntity
import com.farbalapps.rinde.data.local.dao.GoalsDao

import com.farbalapps.rinde.data.local.entity.PendingVoteEntity
import com.farbalapps.rinde.data.local.dao.PendingVoteDao

import com.farbalapps.rinde.data.local.entity.CommunityPostFtsEntity

import com.farbalapps.rinde.data.local.entity.SavedListEntity
import com.farbalapps.rinde.data.local.dao.SavedListDao

@Database(
    entities = [
        ShoppingItemEntity::class, 
        CustomProductHistoryEntity::class, 
        CategoryEntity::class, 
        ProfileEntity::class,
        CommunityPostEntity::class,
        CommunityPostFtsEntity::class,
        SyncMetadataEntity::class,
        UserVoteEntity::class,
        SavingsGoalEntity::class,
        GoalTransactionEntity::class,
        PendingVoteEntity::class,
        SavedListEntity::class
    ],
    version = 24,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RindeDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun customProductHistoryDao(): CustomProductHistoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun userVoteDao(): UserVoteDao
    abstract fun goalsDao(): GoalsDao
    abstract fun pendingVoteDao(): PendingVoteDao
    abstract fun savedListDao(): SavedListDao
}

