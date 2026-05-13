package com.farbalapps.rinde.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        ShoppingItemEntity::class, 
        CustomProductHistoryEntity::class, 
        CategoryEntity::class, 
        ProfileEntity::class,
        CommunityPostEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class RindeDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun customProductHistoryDao(): CustomProductHistoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
}
