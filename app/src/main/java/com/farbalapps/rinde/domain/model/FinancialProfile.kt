package com.farbalapps.rinde.domain.model

/**
 * Frecuencia de percepción del ingreso del usuario.
 */
enum class IncomeFrequency {
    MONTHLY,
    BIWEEKLY,
    WEEKLY,
    DAILY,
    CUSTOM;

    companion object {
        fun fromString(value: String): IncomeFrequency {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

/**
 * Modelo de dominio que representa el perfil financiero y el ingreso base del usuario.
 */
data class FinancialProfile(
    val id: String = "",
    val income: Double = 0.0,
    val incomeFrequency: IncomeFrequency = IncomeFrequency.MONTHLY,
    val currency: String = "MXN",
    val updatedAt: Long = System.currentTimeMillis(),
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val isVariableIncome: Boolean = false
) {
    /**
     * Días que abarca el periodo personalizado (mínimo 1).
     */
    val customPeriodDays: Int
        get() {
            if (customStartDate == null || customEndDate == null || customEndDate < customStartDate) return 30
            val diff = customEndDate - customStartDate
            return kotlin.math.max(1, (diff / (1000L * 60 * 60 * 24)).toInt() + 1)
        }

    /**
     * Retorna el ingreso normalizado a base mensual para cálculo comparativo con gastos del mes.
     */
    val monthlyEquivalent: Double
        get() = when (incomeFrequency) {
            IncomeFrequency.MONTHLY -> income
            IncomeFrequency.BIWEEKLY -> income * 2.0
            IncomeFrequency.WEEKLY -> income * (52.0 / 12.0)
            IncomeFrequency.DAILY -> income * 30.0
            IncomeFrequency.CUSTOM -> (income / customPeriodDays) * 30.0
        }

    /**
     * Retorna la tasa diaria equivalente del ingreso.
     */
    val dailyIncome: Double
        get() = monthlyEquivalent / 30.0
}
