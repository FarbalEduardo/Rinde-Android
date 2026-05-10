package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories", primaryKeys = ["name", "userId"])
data class CategoryEntity(
    val name: String,
    val userId: String,
    val isCustom: Boolean = true,
    val orderIndex: Int = 0
)
