package com.farbalapps.rinde.data.local.mapper

import com.farbalapps.rinde.data.local.entity.ShoppingItemEntity
import com.farbalapps.rinde.domain.model.ShoppingItem

/**
 * Extension functions to map between domain and data models.
 */

fun ShoppingItemEntity.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    name = name,
    category = category,
    isCompleted = isCompleted,
    quantity = quantity,
    unit = unit,
    emoji = emoji,
    listGroup = listGroup,
    userId = userId
)

fun ShoppingItem.toEntity(userId: String): ShoppingItemEntity = ShoppingItemEntity(
    id = id,
    name = name,
    category = category,
    isCompleted = isCompleted,
    quantity = quantity,
    unit = unit,
    emoji = emoji,
    listGroup = listGroup,
    userId = userId
)
