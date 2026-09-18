package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para persistir el historial consolidado de cada mes.
 * Permite que cada mes mantenga sus datos independientes y el usuario
 * pueda consultar su historial financiero sin sobreescribir ni mezclar
 * la información entre meses.
 */
@Entity(
    tableName = "monthly_financial_records",
    indices = [
        Index(value = ["userId", "year", "month"], unique = true)
    ]
)
data class MonthlyFinancialRecordEntity(
    @PrimaryKey val id: String = "", // Formato: "${userId}_${year}_${month}"
    val userId: String = "",
    val year: Int = 2026,
    val month: Int = 1, // 1-12
    val income: Double = 0.0,
    val extraExpensesTotal: Double = 0.0,
    val listTotal: Double = 0.0,
    val goalsCommittedTotal: Double = 0.0,
    val availableAmount: Double = 0.0,
    val healthStatus: String = "GOOD",
    val isClosed: Boolean = false,
    val updatedAt: Long = 0L
)
