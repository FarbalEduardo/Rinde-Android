package com.farbalapps.rinde.domain.model

/**
 * Domain model representing a saved shopping list backup/snapshot.
 *
 * @param id Unique identifier for the saved list.
 * @param name Custom name for the saved list.
 * @param savedAt Timestamp when the list was originally saved.
 * @param lastModifiedAt Timestamp when the list was last renamed/modified.
 * @param totalItems Total number of items in the list.
 * @param completedItems Total number of completed items at time of saving.
 * @param totalPrice Sum of item prices (null if no item has price).
 * @param currency Currency code for the total (e.g. "MXN").
 * @param items Snapshot of shopping items.
 * @param userId Firebase UID of the owner.
 */
data class SavedShoppingList(
    val id: String = "",
    val name: String,
    val savedAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = savedAt,
    val totalItems: Int,
    val completedItems: Int,
    val totalPrice: Double? = null,
    val currency: String = "MXN",
    val items: List<ShoppingItem> = emptyList(),
    val userId: String = ""
)
