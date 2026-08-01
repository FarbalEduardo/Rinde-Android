package com.farbalapps.rinde.data.repository

import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.farbalapps.rinde.data.local.dao.GoalsDao
import com.farbalapps.rinde.data.local.entity.GoalTransactionEntity
import com.farbalapps.rinde.data.local.entity.SavingsGoalEntity
import com.farbalapps.rinde.data.local.mapper.toDomain
import com.farbalapps.rinde.data.local.mapper.toEntity
import com.farbalapps.rinde.data.worker.SyncGoalsWorker
import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.model.GoalTransaction
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseGoalsRepository @Inject constructor(
    private val dao: GoalsDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GoalsRepository {

    companion object {
        private const val TAG = "FirebaseGoalsRepository"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun getGoals(): Flow<List<SavingsGoal>> {
        val userId = currentUserId ?: return emptyFlow()
        return dao.getGoalsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getGoalById(goalId: String): Flow<SavingsGoal?> {
        return dao.observeGoalById(goalId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getGoalsSnapshot(): List<SavingsGoal> = withContext(ioDispatcher) {
        val userId = currentUserId ?: return@withContext emptyList()
        dao.getGoalsByUserSnapshot(userId).map { it.toDomain() }
    }

    override suspend fun createGoal(goal: SavingsGoal) = withContext(ioDispatcher) {
        val userId = currentUserId ?: throw Exception("User not logged in")
        var entity = goal.copy(userId = userId).toEntity(isSynced = false)
        dao.insertGoal(entity)
        
        // Subida directa para que sea instantáneo. Si falla, arrojará excepción al ViewModel
        firestore.collection("users")
            .document(userId)
            .collection("savings_goals")
            .document(entity.id)
            .set(entity, com.google.firebase.firestore.SetOptions.merge())
            .await()
        
        // Si tiene éxito, marcamos como sincronizado
        entity = entity.copy(isSynced = true)
        dao.updateGoal(entity)
        
        enqueueSync()
    }

    override suspend fun deleteGoal(goalId: String) = withContext(ioDispatcher) {
        dao.deleteGoalById(goalId)
        
        val userId = currentUserId
        if (userId != null) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("savings_goals")
                    .document(goalId)
                    .delete()
                    // No usamos await() aquí intencionalmente. Si estamos offline, 
                    // el SDK de Firebase encolará la eliminación y la ejecutará
                    // cuando volvamos a estar online.
            } catch (e: Exception) {
                Log.e(TAG, "Error trying to delete goal from Firestore", e)
            }
        }
        
        // Sigue siendo útil llamar a enqueueSync por si hay otras metas pendientes de subir
        enqueueSync()
    }

    override suspend fun depositToGoal(goalId: String, amount: Double, note: String) = withContext(ioDispatcher) {
        val goalEntity = dao.getGoalById(goalId) ?: throw NoSuchElementException("Meta no encontrada")
        
        // Calcular nuevo monto con tope de 100% de la meta (Regla E3.4 cap)
        val newAmount = (goalEntity.currentAmount + amount).coerceAtMost(goalEntity.targetAmount)
        val isCompletedNow = newAmount >= goalEntity.targetAmount
        val now = System.currentTimeMillis()

        val updatedGoal = goalEntity.copy(
            currentAmount = newAmount,
            isCompleted = isCompletedNow,
            updatedAt = now,
            isSynced = false
        )
        dao.updateGoal(updatedGoal)

        // Insertar transacción histórica
        val tx = GoalTransactionEntity(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            amount = amount,
            note = note,
            timestamp = now,
            isSynced = false
        )
        dao.insertTransaction(tx)

        enqueueSync()
    }

    override suspend fun updateGoal(goal: SavingsGoal) = withContext(ioDispatcher) {
        val userId = currentUserId ?: throw Exception("User not logged in")
        val entity = goal.copy(userId = userId).toEntity(isSynced = false)
        dao.updateGoal(entity)
        enqueueSync()
    }

    override suspend fun withdrawFromGoal(goalId: String, amount: Double, note: String) = withContext(ioDispatcher) {
        val goalEntity = dao.getGoalById(goalId) ?: throw NoSuchElementException("Meta no encontrada")
        
        val newAmount = (goalEntity.currentAmount - amount).coerceAtLeast(0.0)
        val isCompletedNow = newAmount >= goalEntity.targetAmount
        val now = System.currentTimeMillis()

        val updatedGoal = goalEntity.copy(
            currentAmount = newAmount,
            isCompleted = isCompletedNow,
            updatedAt = now,
            isSynced = false
        )
        dao.updateGoal(updatedGoal)

        // Insertar transacción histórica como monto negativo
        val tx = GoalTransactionEntity(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            amount = -amount,
            note = note,
            timestamp = now,
            isSynced = false
        )
        dao.insertTransaction(tx)

        enqueueSync()
    }

    override fun getTransactionsForGoal(goalId: String): Flow<List<GoalTransaction>> {
        return dao.getTransactionsForGoal(goalId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncGoals() {
        enqueueSync()
    }

    private fun enqueueSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncGoalsWorker>().build()
        workManager.enqueue(syncRequest)
        Log.d(TAG, "SyncGoalsWorker enqueued.")
    }
}
