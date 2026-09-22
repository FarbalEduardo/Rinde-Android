package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
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
        customEndDate: Long? = null,
        isVariableIncome: Boolean = false
    )

    /**
     * Observa el sueldo variable configurado para un mes y año específicos.
     */
    fun getMonthlyVariableIncome(year: Int, month: Int): Flow<Double?>

    /**
     * Guarda el sueldo variable para un mes y año específicos, incluyendo la fecha de cobro.
     */
    suspend fun saveMonthlyVariableIncome(
        year: Int,
        month: Int,
        amount: Double,
        paymentDate: Long = System.currentTimeMillis()
    )

    /**
     * Observa la lista de ganancias o ingresos extra registrados en el año y mes dados.
     */
    fun getExtraIncomes(year: Int, month: Int): Flow<List<ExtraIncome>>

    /**
     * Observa la lista de ganancias o ingresos extra registrados dentro de un rango de timestamps.
     */
    fun getExtraIncomesBetween(startTime: Long, endTime: Long): Flow<List<ExtraIncome>>

    /**
     * Registra una nueva ganancia extra (Ventas, bono, etc.) para el mes especificado y fecha dada.
     */
    suspend fun addExtraIncome(
        label: String,
        amount: Double,
        iconKey: String,
        year: Int,
        month: Int,
        incomeDate: Long = System.currentTimeMillis()
    )

    /**
     * Elimina una ganancia extra por su ID.
     */
    suspend fun deleteExtraIncome(id: String)

    /**
     * Actualiza el concepto, monto y opcionalmente la fecha de una ganancia extra existente.
     */
    suspend fun updateExtraIncome(id: String, label: String, amount: Double, incomeDate: Long? = null)

    /**
     * Observa la lista de gastos extra registrados en el año y mes dados.
     */
    fun getExtraExpenses(year: Int, month: Int): Flow<List<ExtraExpense>>

    /**
     * Observa la lista de gastos extra registrados dentro de un rango de timestamps.
     */
    fun getExtraExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExtraExpense>>

    /**
     * Registra un nuevo gasto extra (Luz, Agua, Renta, etc.) para el mes especificado y fecha programada.
     */
    suspend fun addExtraExpense(
        label: String,
        amount: Double,
        iconKey: String,
        year: Int,
        month: Int,
        expenseDate: Long = System.currentTimeMillis()
    )

    /**
     * Elimina un gasto extra por su ID.
     */
    suspend fun deleteExtraExpense(id: String)

    /**
     * Actualiza el concepto, monto y opcionalmente la fecha programada de un gasto extra existente.
     */
    suspend fun updateExtraExpense(id: String, label: String, amount: Double, expenseDate: Long? = null)

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
