package com.farbalapps.rinde.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.farbalapps.rinde.data.local.dao.GoalsDao
import com.farbalapps.rinde.data.local.entity.SavingsGoalEntity
import com.farbalapps.rinde.data.local.entity.GoalTransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException

import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncGoalsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: GoalsDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncGoalsWorker"
    }

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()

        try {
            // 1. Sincronizar Metas no sincronizadas
            val unsyncedGoals = dao.getUnsyncedGoals()
            for (goal in unsyncedGoals) {
                uploadGoal(userId, goal)
            }

            // 2. Sincronizar Transacciones no sincronizadas
            val unsyncedTx = dao.getUnsyncedTransactions()
            for (tx in unsyncedTx) {
                uploadTransaction(userId, tx)
            }

            // 3. Traer actualizaciones desde Firebase (E7.4 Sync de dispositivos)
            fetchRemoteGoals(userId)

            return Result.success()
        } catch (e: IOException) {
            Log.e(TAG, "Network error syncing goals, retrying...", e)
            return Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during goal sync", e)
            return Result.failure()
        }
    }

    private suspend fun uploadGoal(userId: String, goal: SavingsGoalEntity) {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("savings_goals")
            .document(goal.id)

        // Usar merge para evitar sobreescritura accidental o colisiones de conexión intermitente (E7.3)
        docRef.set(goal, SetOptions.merge()).await()
        dao.updateGoal(goal.copy(isSynced = true))
    }

    private suspend fun uploadTransaction(userId: String, tx: GoalTransactionEntity) {
        firestore.collection("users")
            .document(userId)
            .collection("savings_goals")
            .document(tx.goalId)
            .collection("transactions")
            .document(tx.id)
            .set(tx, SetOptions.merge())
            .await()
        
        dao.insertTransaction(tx.copy(isSynced = true))
    }

    private suspend fun fetchRemoteGoals(userId: String) {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("savings_goals")
            .get()
            .await()

        for (doc in snapshot.documents) {
            val remoteGoal = doc.toObject(SavingsGoalEntity::class.java)
            if (remoteGoal != null) {
                val localGoal = dao.getGoalById(remoteGoal.id)
                // Si no existe localmente o el remoto es más nuevo, actualiza Room
                if (localGoal == null || remoteGoal.updatedAt > localGoal.updatedAt) {
                    dao.insertGoal(remoteGoal.copy(isSynced = true))
                }
            }
        }
    }
}
