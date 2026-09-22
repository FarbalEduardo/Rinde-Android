package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para registrar ganancias o ingresos adicionales (Ventas ocasionales, segundo trabajo, bonos, etc.)
 */
@Entity(
    tableName = "extra_incomes",
    indices = [
        Index(value = ["userId", "year", "month"]),
        Index(value = ["userId", "incomeDate"])
    ]
)
data class ExtraIncomeEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val label: String = "",
    val amount: Double = 0.0,
    val iconKey: String = "payments",
    val month: Int = 1,
    val year: Int = 2026,
    val createdAt: Long = 0L,
    val incomeDate: Long = 0L
)
