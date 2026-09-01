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
            // 1. Sincronizar Metas no sincronizadas del usuario actual exclusivamente
            val unsyncedGoals = dao.getUnsyncedGoalsByUser(userId)
            for (goal in unsyncedGoals) {
                uploadGoal(userId, goal)
            }

            // 2. Sincronizar Transacciones no sincronizadas del usuario actual exclusivamente
            val unsyncedTx = dao.getUnsyncedTransactionsByUser(userId)
            for (tx in unsyncedTx) {
                uploadTransaction(userId, tx)
            }

            // 3. Traer actualizaciones desde Firebase
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
        if (goal.userId != userId) {
            Log.w(TAG, "Skipping cross-user upload: goal.userId (${goal.userId}) != currentUserId ($userId)")
            return
        }

        val docRef = firestore.collection("users")
            .document(userId)
            .collection("savings_goals")
            .document(goal.id)

        docRef.set(goal, SetOptions.merge()).await()
        dao.updateGoal(goal.copy(isSynced = true))
    }

    private suspend fun uploadTransaction(userId: String, tx: GoalTransactionEntity) {
        val goal = dao.getGoalById(tx.goalId)
        if (goal != null && goal.userId != userId) {
            Log.w(TAG, "Skipping cross-user transaction upload: goal.userId (${goal.userId}) != currentUserId ($userId)")
            return
        }

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
            var remoteGoal = doc.toObject(SavingsGoalEntity::class.java)
            if (remoteGoal != null) {
                // Migración: documentos antiguos almacenaban isArchived como "archived"
                // (bug de serialización Kotlin/Firestore). Leemos ambos campos para compatibilidad.
                val legacyArchived = doc.getBoolean("archived") ?: false
                val legacyCompleted = doc.getBoolean("completed") ?: false
                val legacySynced = doc.getBoolean("synced") ?: false

                // Si el documento tiene el campo legacy y el valor actual es false,
                // es porque se leyó del campo incorrecto. Aplicamos el valor correcto.
                if (legacyArchived && !remoteGoal.isArchived) {
                    remoteGoal = remoteGoal.copy(isArchived = true)
                }
                if (legacyCompleted && !remoteGoal.isCompleted) {
                    remoteGoal = remoteGoal.copy(isCompleted = true)
                }

                val goalToInsert = remoteGoal.copy(userId = userId, isSynced = true)
                val localGoal = dao.getGoalById(remoteGoal.id)

                // Si no existe localmente o el remoto es más nuevo, actualiza Room
                if (localGoal == null || goalToInsert.updatedAt > localGoal.updatedAt) {
                    dao.insertGoal(goalToInsert)
                }

                // Si había campos legacy, re-subir a Firestore con los nombres correctos
                if (legacyArchived || legacyCompleted || legacySynced) {
                    try {
                        firestore.collection("users")
                            .document(userId)
                            .collection("savings_goals")
                            .document(goalToInsert.id)
                            .set(goalToInsert, SetOptions.merge())
                            .await()
                        Log.d(TAG, "Migrated legacy boolean fields for goal: ${goalToInsert.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not migrate goal ${goalToInsert.id}: ${e.message}")
                    }
                }
            }
        }
    }
}
