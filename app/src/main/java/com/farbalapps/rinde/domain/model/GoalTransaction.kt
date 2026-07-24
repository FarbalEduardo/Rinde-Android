package com.farbalapps.rinde.domain.model

/**
 * Modelo de negocio que representa una transacción de depósito/ahorro para una meta.
 *
 * @property id Identificador único de la transacción (UUID).
 * @property goalId Identificador de la meta a la que pertenece el depósito.
 * @property amount Cantidad depositada en esta transacción.
 * @property note Comentario o nota opcional del depósito. Soporta emojis y caracteres especiales.
 * @property timestamp Marca de tiempo en milisegundos de la transacción.
 */
data class GoalTransaction(
    val id: String,
    val goalId: String,
    val amount: Double,
    val note: String,
    val timestamp: Long
)
