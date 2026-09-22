package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

/**
 * Entidad de Room para persistir las Metas de Ahorro.
 *
 * NOTA: Los campos Boolean con prefijo "is" usan @get:PropertyName y @set:PropertyName
 * para que Firestore serialice/deserialice correctamente el nombre del campo.
 * Sin esto, Firestore convierte isArchived → "archived" (quitando el "is"),
 * lo cual hace que al leer el documento, isArchived siempre sea false.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: Long? = null,
    val iconKey: String = "",
    val colorKey: String = "",
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @get:PropertyName("isArchived") @set:PropertyName("isArchived")
    var isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val monthlySnapshotAmount: Double = 0.0,
    @get:PropertyName("isSynced") @set:PropertyName("isSynced")
    var isSynced: Boolean = false
)

