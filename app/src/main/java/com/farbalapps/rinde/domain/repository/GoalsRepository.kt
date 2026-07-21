package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.model.GoalTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Repositorio para la gestión de Metas de Ahorro y sus Transacciones.
 * Implementa el patrón Repository de Clean Architecture.
 */
interface GoalsRepository {

    /**
     * Retorna un Flow reactivo con la lista de metas del usuario actual.
     */
    fun getGoals(): Flow<List<SavingsGoal>>

    /**
     * Retorna una instantánea (snapshot) de la lista de metas para validaciones sincrónicas.
     */
    suspend fun getGoalsSnapshot(): List<SavingsGoal>

    /**
     * Registra una nueva meta de ahorro en local y la encola para sincronizar.
     */
    suspend fun createGoal(goal: SavingsGoal)

    /**
     * Elimina una meta de ahorro por su identificador.
     */
    suspend fun deleteGoal(goalId: String)

    /**
     * Registra un depósito monetario sobre una meta y actualiza su estado.
     */
    suspend fun depositToGoal(goalId: String, amount: Double, note: String)

    /**
     * Obtiene el flujo de transacciones registradas para una meta específica.
     */
    fun getTransactionsForGoal(goalId: String): Flow<List<GoalTransaction>>

    /**
     * Dispara manualmente la sincronización con el servidor remoto (Firestore).
     */
    suspend fun syncGoals()
}
