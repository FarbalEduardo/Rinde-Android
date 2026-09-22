package com.farbalapps.rinde.domain.usecase.dashboard

import com.farbalapps.rinde.domain.repository.DashboardRepository
import javax.inject.Inject

/**
 * Caso de uso para gestionar la creación, actualización y eliminación de ganancias o ingresos extras.
 */
class ManageExtraIncomeUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend fun addIncome(
        label: String,
        amount: Double,
        iconKey: String = "cash",
        year: Int,
        month: Int,
        incomeDate: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isBlank()) {
            return Result.failure(IllegalArgumentException("El concepto no puede estar vacío"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a cero"))
        }

        return runCatching {
            dashboardRepository.addExtraIncome(
                label = trimmedLabel,
                amount = amount,
                iconKey = iconKey,
                year = year,
                month = month,
                incomeDate = incomeDate
            )
        }
    }

    suspend fun updateIncome(
        id: String,
        label: String,
        amount: Double,
        incomeDate: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isBlank()) {
            return Result.failure(IllegalArgumentException("El concepto no puede estar vacío"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a cero"))
        }

        return runCatching {
            dashboardRepository.updateExtraIncome(
                id = id,
                label = trimmedLabel,
                amount = amount,
                incomeDate = incomeDate
            )
        }
    }

    suspend fun deleteIncome(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de ganancia extra inválido"))
        }
        return runCatching {
            dashboardRepository.deleteExtraIncome(id)
        }
    }
}
