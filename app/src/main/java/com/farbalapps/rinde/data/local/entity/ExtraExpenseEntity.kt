package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para registrar gastos fijos o adicionales del hogar (Luz, Agua, Internet, Renta, etc.)
 */
@Entity(
    tableName = "extra_expenses",
    indices = [
        Index(value = ["userId", "year", "month"])
    ]
)
data class ExtraExpenseEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val label: String = "",
    val amount: Double = 0.0,
    val iconKey: String = "receipt",
    val month: Int = 1,
    val year: Int = 2026,
    val createdAt: Long = 0L
)
