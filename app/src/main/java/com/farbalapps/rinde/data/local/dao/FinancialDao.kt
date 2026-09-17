package com.farbalapps.rinde.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farbalapps.rinde.data.local.entity.ExtraExpenseEntity
import com.farbalapps.rinde.data.local.entity.FinancialProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder y modificar el perfil financiero y los gastos adicionales.
 */
@Dao
interface FinancialDao {

    @Query("SELECT * FROM financial_profiles WHERE id = :userId LIMIT 1")
    fun getFinancialProfile(userId: String): Flow<FinancialProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFinancialProfile(profile: FinancialProfileEntity)

    @Query("SELECT * FROM extra_expenses WHERE userId = :userId AND year = :year AND month = :month ORDER BY createdAt DESC")
    fun getExtraExpenses(userId: String, year: Int, month: Int): Flow<List<ExtraExpenseEntity>>

    @Query("SELECT * FROM extra_expenses WHERE userId = :userId AND createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getExtraExpensesBetween(userId: String, startTime: Long, endTime: Long): Flow<List<ExtraExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtraExpense(expense: ExtraExpenseEntity)

    @Query("DELETE FROM extra_expenses WHERE id = :expenseId")
    suspend fun deleteExtraExpense(expenseId: String)

    @Query("UPDATE extra_expenses SET label = :label, amount = :amount WHERE id = :id")
    suspend fun updateExtraExpense(id: String, label: String, amount: Double)

    @Query("DELETE FROM financial_profiles WHERE id = :userId")
    suspend fun deleteProfileByUserId(userId: String)

    @Query("DELETE FROM extra_expenses WHERE userId = :userId")
    suspend fun deleteExpensesByUserId(userId: String)

    @Query("DELETE FROM financial_profiles")
    suspend fun clearAllFinancialProfiles()

    @Query("DELETE FROM extra_expenses")
    suspend fun clearAllExtraExpenses()
}
