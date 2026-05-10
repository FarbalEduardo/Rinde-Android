package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for custom product history.
 * 
 * @param name Product name.
 * @param category Product category.
 * @param addedAt Timestamp when the product was added.
 */
@Entity(tableName = "custom_product_history")
data class CustomProductHistoryEntity(
    @PrimaryKey val name: String,
    val category: String,
    val addedAt: Long = System.currentTimeMillis()
)
