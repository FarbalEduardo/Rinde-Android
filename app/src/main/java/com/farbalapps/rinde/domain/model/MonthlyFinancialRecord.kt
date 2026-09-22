package com.farbalapps.rinde.domain.model

/**
 * Modelo de dominio que representa el registro consolidado e histórico
 * de un mes en particular.
 */
data class MonthlyFinancialRecord(
    val id: String = "",
    val userId: String = "",
    val year: Int,
    val month: Int,
    val income: Double = 0.0,
    val extraExpensesTotal: Double = 0.0,
    val listTotal: Double = 0.0,
    val goalsCommittedTotal: Double = 0.0,
    val availableAmount: Double = 0.0,
    val healthStatus: String = "GOOD",
    val isClosed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
