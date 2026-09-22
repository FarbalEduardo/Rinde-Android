package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para registrar el sueldo variable correspondiente a un mes y año específicos.
 */
@Entity(
    tableName = "monthly_incomes",
    indices = [
        Index(value = ["userId", "year", "month"], unique = true)
    ]
)
data class MonthlyIncomeEntity(
    @PrimaryKey val id: String = "", // Formato: "${userId}_${year}_${month}"
    val userId: String = "",
    val year: Int = 2026,
    val month: Int = 1,
    val amount: Double = 0.0,
    val updatedAt: Long = 0L,
    val paymentDate: Long = 0L
)
