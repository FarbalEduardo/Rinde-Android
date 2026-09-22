package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para persistir el perfil financiero del usuario (ingreso y periodicidad).
 */
@Entity(tableName = "financial_profiles")
data class FinancialProfileEntity(
    @PrimaryKey val id: String = "", // userId
    val income: Double = 0.0,
    val incomeFrequency: String = "MONTHLY",
    val currency: String = "MXN",
    val updatedAt: Long = 0L,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val isVariableIncome: Boolean = false
)
