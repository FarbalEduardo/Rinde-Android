package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farbalapps.rinde.domain.model.ShoppingItem

/**
 * Room entity representing a saved shopping list backup in the local database.
 */
@Entity(tableName = "saved_lists")
data class SavedListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val savedAt: Long,
    val lastModifiedAt: Long,
    val totalItems: Int,
    val completedItems: Int,
    val totalPrice: Double?,
    val currency: String = "MXN",
    val items: List<ShoppingItem>,
    val userId: String
)
