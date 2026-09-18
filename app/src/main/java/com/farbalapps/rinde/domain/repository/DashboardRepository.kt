package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio para la gestión del perfil financiero y gastos extra del dashboard.
 */
interface DashboardRepository {

    /**
     * Observa el perfil de ingresos configurado por el usuario.
     */
    fun getFinancialProfile(): Flow<FinancialProfile?>

    /**
     * Guarda o actualiza el perfil de ingresos y su periodicidad (incluyendo fechas personalizadas si aplica).
     */
    suspend fun saveFinancialProfile(
        income: Double,
        frequency: IncomeFrequency,
        currency: String = "MXN",
        customStartDate: Long? = null,
        customEndDate: Long? = null
    )

    /**
     * Observa la lista de gastos extra registrados en el año y mes dados.
     */
    fun getExtraExpenses(year: Int, month: Int): Flow<List<ExtraExpense>>

    /**
     * Observa la lista de gastos extra registrados dentro de un rango de timestamps.
     */
    fun getExtraExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExtraExpense>>

    /**
     * Registra un nuevo gasto extra (Luz, Agua, Renta, etc.) para el mes especificado.
     */
    suspend fun addExtraExpense(
        label: String,
        amount: Double,
        iconKey: String,
        year: Int,
        month: Int
    )

    /**
     * Elimina un gasto extra por su ID.
     */
    suspend fun deleteExtraExpense(id: String)

    /**
     * Actualiza el concepto y monto de un gasto extra existente.
     */
    suspend fun updateExtraExpense(id: String, label: String, amount: Double)

    /**
     * Sincroniza el perfil financiero y los gastos extra del usuario desde Firebase Firestore a Room.
     */
    suspend fun syncFromFirebase()

    /**
     * Observa el registro consolidado del mes y año solicitados.
     */
    fun getMonthlyRecord(year: Int, month: Int): Flow<com.farbalapps.rinde.domain.model.MonthlyFinancialRecord?>

    /**
     * Guarda o actualiza el registro consolidado de un mes en Room y Firestore.
     */
    suspend fun saveMonthlyRecord(record: com.farbalapps.rinde.domain.model.MonthlyFinancialRecord)

    /**
     * Verifica si se ha cambiado de mes para consolidar y cerrar el mes anterior,
     * sincronizando el snapshot en Room y Firebase y preparando el mes nuevo.
     */
    suspend fun checkAndPerformMonthlyRollover()
}
