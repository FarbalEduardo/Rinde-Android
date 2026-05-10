package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a shopping item in the local database.
 *
 * @param id Unique identifier (UUID string).
 * @param name Product name.
 * @param category String name of the ProductCategory enum.
 * @param isCompleted Whether the item has been purchased.
 * @param userId Firebase UID of the owner.
 */
@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val isCompleted: Boolean = false,
    val quantity: Double,
    val unit: String,
    val emoji: String,
    val listGroup: String = "All",
    val userId: String
)
