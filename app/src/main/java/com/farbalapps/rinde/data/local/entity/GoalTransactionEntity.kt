package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de Room para el historial de depósitos/transacciones realizadas en las metas.
 */
@Entity(
    tableName = "goal_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class GoalTransactionEntity(
    @PrimaryKey val id: String = "",
    val goalId: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val timestamp: Long = 0L,
    val isSynced: Boolean = false
)
