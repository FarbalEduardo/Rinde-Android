package com.farbalapps.rinde.domain.model

import java.util.UUID

/**
 * Modelo de dominio para ingresos o ganancias adicionales del usuario
 * (Ventas ocasionales, segundo trabajo, bonos, comisiones, etc.).
 */
data class ExtraIncome(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val label: String,
    val amount: Double,
    val iconKey: String = "payments",
    val month: Int,
    val year: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val incomeDate: Long = createdAt
)
