package com.farbalapps.rinde.domain.usecase.dashboard

import com.farbalapps.rinde.domain.repository.DashboardRepository
import javax.inject.Inject

/**
 * Caso de uso para gestionar la creación, actualización y eliminación de gastos del hogar.
 */
class ManageExtraExpenseUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend fun addExpense(
        label: String,
        amount: Double,
        iconKey: String = "receipt",
        year: Int,
        month: Int,
        expenseDate: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isBlank()) {
            return Result.failure(IllegalArgumentException("El concepto no puede estar vacío"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a cero"))
        }

        return runCatching {
            dashboardRepository.addExtraExpense(
                label = trimmedLabel,
                amount = amount,
                iconKey = iconKey,
                year = year,
                month = month,
                expenseDate = expenseDate
            )
        }
    }

    suspend fun updateExpense(
        id: String,
        label: String,
        amount: Double,
        expenseDate: Long = System.currentTimeMillis()
    ): Result<Unit> {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isBlank()) {
            return Result.failure(IllegalArgumentException("El concepto no puede estar vacío"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a cero"))
        }

        return runCatching {
            dashboardRepository.updateExtraExpense(
                id = id,
                label = trimmedLabel,
                amount = amount,
                expenseDate = expenseDate
            )
        }
    }

    suspend fun deleteExpense(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de gasto inválido"))
        }
        return runCatching {
            dashboardRepository.deleteExtraExpense(id)
        }
    }
}
