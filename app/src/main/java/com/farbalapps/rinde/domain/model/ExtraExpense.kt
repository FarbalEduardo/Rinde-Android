package com.farbalapps.rinde.domain.model

import java.util.UUID

/**
 * Modelo de dominio para gastos fijos o adicionales del usuario (Luz, Agua, Internet, Renta, etc.)
 */
data class ExtraExpense(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val label: String,
    val amount: Double,
    val iconKey: String = "receipt",
    val month: Int,
    val year: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val expenseDate: Long = createdAt
)
