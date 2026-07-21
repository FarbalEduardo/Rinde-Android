package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para persistir las Metas de Ahorro.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val iconKey: String,
    val colorKey: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val monthlySnapshotAmount: Double,
    val isSynced: Boolean = false
)
