package com.farbalapps.rinde.data.local.dao

import androidx.room.*
import com.farbalapps.rinde.data.local.entity.SavingsGoalEntity
import com.farbalapps.rinde.data.local.entity.GoalTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) de Room para interactuar con la persistencia local de Metas de Ahorro y Transacciones.
 */
@Dao
interface GoalsDao {

    @Query("SELECT * FROM savings_goals WHERE userId = :userId AND isArchived = 0 ORDER BY orderIndex ASC")
    fun getGoalsByUser(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedGoalsByUser(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId AND isArchived = 0 ORDER BY orderIndex ASC")
    suspend fun getGoalsByUserSnapshot(userId: String): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE id = :id AND userId = :userId LIMIT 1")
    fun observeGoalById(id: String, userId: String): Flow<SavingsGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: String)

    @Query("DELETE FROM savings_goals WHERE userId = :userId")
    suspend fun deleteGoalsByUserId(userId: String)

    @Query("DELETE FROM goal_transactions WHERE goalId IN (SELECT id FROM savings_goals WHERE userId = :userId)")
    suspend fun deleteTransactionsByUserId(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: GoalTransactionEntity)

    @Query("SELECT * FROM goal_transactions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getTransactionsForGoal(goalId: String): Flow<List<GoalTransactionEntity>>

    @Query("SELECT * FROM savings_goals WHERE isSynced = 0")
    suspend fun getUnsyncedGoals(): List<SavingsGoalEntity>

    @Query("SELECT * FROM goal_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<GoalTransactionEntity>
}
