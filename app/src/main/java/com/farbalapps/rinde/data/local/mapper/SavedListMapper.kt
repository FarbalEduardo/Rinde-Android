package com.farbalapps.rinde.data.local.mapper

import com.farbalapps.rinde.data.local.entity.SavedListEntity
import com.farbalapps.rinde.domain.model.SavedShoppingList

fun SavedListEntity.toDomain(): SavedShoppingList = SavedShoppingList(
    id = id,
    name = name,
    savedAt = savedAt,
    lastModifiedAt = lastModifiedAt,
    totalItems = totalItems,
    completedItems = completedItems,
    totalPrice = totalPrice,
    currency = currency,
    items = items,
    userId = userId
)

fun SavedShoppingList.toEntity(userId: String): SavedListEntity = SavedListEntity(
    id = id,
    name = name,
    savedAt = savedAt,
    lastModifiedAt = lastModifiedAt,
    totalItems = totalItems,
    completedItems = completedItems,
    totalPrice = totalPrice,
    currency = currency,
    items = items,
    userId = userId
)
