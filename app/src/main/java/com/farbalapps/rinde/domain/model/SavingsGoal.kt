package com.farbalapps.rinde.domain.model

/**
 * Modelo de negocio que representa una Meta de Ahorro.
 *
 * @property id Identificador único (UUID).
 * @property userId ID del usuario dueño de la meta.
 * @property title Título de la meta (límite de 30 caracteres).
 * @property targetAmount Monto objetivo final a alcanzar.
 * @property currentAmount Monto acumulado actual (con tope en [targetAmount]).
 * @property iconKey Identificador del icono predefinido.
 * @property colorKey Identificador del color temático de la tarjeta.
 * @property isCompleted Estado de completado de la meta.
 * @property createdAt Timestamp de creación.
 * @property updatedAt Timestamp de la última modificación.
 * @property monthlySnapshotAmount Monto acumulado al inicio del mes para cálculos de delta de crecimiento.
 */
data class SavingsGoal(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long?, // Nueva fecha límite de la meta
    val iconKey: String,
    val colorKey: String,
    val isCompleted: Boolean,
    val isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val monthlySnapshotAmount: Double
)
