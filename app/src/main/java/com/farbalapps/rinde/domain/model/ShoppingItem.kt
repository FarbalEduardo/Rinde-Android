package com.farbalapps.rinde.domain.model

/**
 * Domain model representing a shopping list item.
 * This is a pure Kotlin class with no Android/UI/Firebase dependencies.
 *
 * @param id Unique identifier for the item.
 * @param name Display name of the product.
 * @param category String representation of the product category.
 * @param isCompleted Whether the item has been purchased.
 * @param quantity Numeric quantity (e.g. 1.0, 500).
 * @param unit Unit description (e.g. "Pieza", "Kg").
 * @param emoji Visual icon for the item.
 * @param userId The UID of the user who owns this item.
 */
data class ShoppingItem(
    val id: String = "",
    val name: String,
    val category: String,
    val isCompleted: Boolean = false,
    val quantity: Double = 1.0,
    val unit: String = "Pieza",
    val emoji: String = "",
    val listGroup: String = "All",
    val userId: String = ""
)
