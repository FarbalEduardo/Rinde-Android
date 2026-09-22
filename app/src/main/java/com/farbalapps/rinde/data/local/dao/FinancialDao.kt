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

    @Query("SELECT * FROM extra_expenses WHERE userId = :userId AND year = :year AND month = :month ORDER BY CASE WHEN expenseDate > 0 THEN expenseDate ELSE createdAt END ASC")
    fun getExtraExpenses(userId: String, year: Int, month: Int): Flow<List<ExtraExpenseEntity>>

    @Query("SELECT * FROM extra_expenses WHERE userId = :userId AND ((expenseDate > 0 AND expenseDate >= :startTime AND expenseDate <= :endTime) OR (expenseDate = 0 AND createdAt >= :startTime AND createdAt <= :endTime)) ORDER BY CASE WHEN expenseDate > 0 THEN expenseDate ELSE createdAt END ASC")
    fun getExtraExpensesBetween(userId: String, startTime: Long, endTime: Long): Flow<List<ExtraExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtraExpense(expense: ExtraExpenseEntity)

    @Query("DELETE FROM extra_expenses WHERE id = :expenseId")
    suspend fun deleteExtraExpense(expenseId: String)

    @Query("UPDATE extra_expenses SET label = :label, amount = :amount, expenseDate = CASE WHEN :expenseDate > 0 THEN :expenseDate ELSE expenseDate END WHERE id = :id")
    suspend fun updateExtraExpense(id: String, label: String, amount: Double, expenseDate: Long = 0L)

    @Query("DELETE FROM financial_profiles WHERE id = :userId")
    suspend fun deleteProfileByUserId(userId: String)

    @Query("DELETE FROM extra_expenses WHERE userId = :userId")
    suspend fun deleteExpensesByUserId(userId: String)

    @Query("SELECT * FROM monthly_financial_records WHERE userId = :userId AND year = :year AND month = :month LIMIT 1")
    fun getMonthlyRecord(userId: String, year: Int, month: Int): Flow<com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity?>

    @Query("SELECT * FROM monthly_financial_records WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getAllMonthlyRecords(userId: String): Flow<List<com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyRecord(record: com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity)

    @Query("DELETE FROM monthly_financial_records WHERE id = :id")
    suspend fun deleteMonthlyRecord(id: String)

    @Query("DELETE FROM monthly_financial_records WHERE userId = :userId")
    suspend fun deleteMonthlyRecordsByUserId(userId: String)

    @Query("DELETE FROM monthly_financial_records")
    suspend fun clearAllMonthlyRecords()

    @Query("DELETE FROM financial_profiles")
    suspend fun clearAllFinancialProfiles()

    @Query("DELETE FROM extra_expenses")
    suspend fun clearAllExtraExpenses()

    // --- Ganancias Extras (Extra Incomes) ---
    @Query("SELECT * FROM extra_incomes WHERE userId = :userId AND year = :year AND month = :month ORDER BY CASE WHEN incomeDate > 0 THEN incomeDate ELSE createdAt END ASC")
    fun getExtraIncomes(userId: String, year: Int, month: Int): Flow<List<com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity>>

    @Query("SELECT * FROM extra_incomes WHERE userId = :userId AND ((incomeDate > 0 AND incomeDate >= :startTime AND incomeDate <= :endTime) OR (incomeDate = 0 AND createdAt >= :startTime AND createdAt <= :endTime)) ORDER BY CASE WHEN incomeDate > 0 THEN incomeDate ELSE createdAt END ASC")
    fun getExtraIncomesBetween(userId: String, startTime: Long, endTime: Long): Flow<List<com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtraIncome(income: com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity)

    @Query("DELETE FROM extra_incomes WHERE id = :incomeId")
    suspend fun deleteExtraIncome(incomeId: String)

    @Query("UPDATE extra_incomes SET label = :label, amount = :amount, incomeDate = CASE WHEN :incomeDate > 0 THEN :incomeDate ELSE incomeDate END WHERE id = :id")
    suspend fun updateExtraIncome(id: String, label: String, amount: Double, incomeDate: Long = 0L)

    @Query("DELETE FROM extra_incomes WHERE userId = :userId")
    suspend fun deleteExtraIncomesByUserId(userId: String)

    @Query("DELETE FROM extra_incomes")
    suspend fun clearAllExtraIncomes()

    // --- Ingresos Variables por Mes (Monthly Incomes) ---
    @Query("SELECT * FROM monthly_incomes WHERE userId = :userId AND year = :year AND month = :month LIMIT 1")
    fun getMonthlyVariableIncome(userId: String, year: Int, month: Int): Flow<com.farbalapps.rinde.data.local.entity.MonthlyIncomeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyVariableIncome(income: com.farbalapps.rinde.data.local.entity.MonthlyIncomeEntity)

    @Query("DELETE FROM monthly_incomes WHERE userId = :userId")
    suspend fun deleteMonthlyIncomesByUserId(userId: String)

    @Query("DELETE FROM monthly_incomes")
    suspend fun clearAllMonthlyIncomes()
}
