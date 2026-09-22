package com.farbalapps.rinde.data.repository

import android.util.Log
import com.farbalapps.rinde.data.local.dao.FinancialDao
import com.farbalapps.rinde.data.local.entity.ExtraExpenseEntity
import com.farbalapps.rinde.data.local.entity.FinancialProfileEntity
import com.farbalapps.rinde.data.local.mapper.toDomain
import com.farbalapps.rinde.data.local.mapper.toEntity
import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dao: FinancialDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DashboardRepository {

    companion object {
        private const val TAG = "DashboardRepositoryImpl"
    }

    private val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty().ifEmpty { "default_user" }

    override fun getFinancialProfile(): Flow<FinancialProfile?> {
        return dao.getFinancialProfile(currentUserId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveFinancialProfile(
        income: Double,
        frequency: IncomeFrequency,
        currency: String,
        customStartDate: Long?,
        customEndDate: Long?,
        isVariableIncome: Boolean
    ) = withContext(ioDispatcher) {
        val profile = FinancialProfile(
            id = currentUserId,
            income = income,
            incomeFrequency = frequency,
            currency = currency,
            updatedAt = System.currentTimeMillis(),
            customStartDate = customStartDate,
            customEndDate = customEndDate,
            isVariableIncome = isVariableIncome
        )
        val entity = profile.toEntity(currentUserId)
        dao.upsertFinancialProfile(entity)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("financial_profile")
                    .document("profile")
                    .set(entity, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing financial profile to Firestore", e)
            }
        }
    }

    override fun getMonthlyVariableIncome(year: Int, month: Int): Flow<Double?> {
        return dao.getMonthlyVariableIncome(currentUserId, year, month).map { it?.amount }
    }

    override suspend fun saveMonthlyVariableIncome(
        year: Int,
        month: Int,
        amount: Double,
        paymentDate: Long
    ) = withContext(ioDispatcher) {
        val entity = com.farbalapps.rinde.data.local.entity.MonthlyIncomeEntity(
            id = "${currentUserId}_${year}_${month}",
            userId = currentUserId,
            year = year,
            month = month,
            amount = amount,
            updatedAt = System.currentTimeMillis(),
            paymentDate = paymentDate
        )
        dao.upsertMonthlyVariableIncome(entity)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("monthly_incomes")
                    .document("${year}_${month}")
                    .set(entity, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing monthly variable income to Firestore", e)
            }
        }
    }

    override fun getExtraIncomes(year: Int, month: Int): Flow<List<ExtraIncome>> {
        return dao.getExtraIncomes(currentUserId, year, month).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getExtraIncomesBetween(startTime: Long, endTime: Long): Flow<List<ExtraIncome>> {
        return dao.getExtraIncomesBetween(currentUserId, startTime, endTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addExtraIncome(
        label: String,
        amount: Double,
        iconKey: String,
        year: Int,
        month: Int,
        incomeDate: Long
    ) = withContext(ioDispatcher) {
        val income = ExtraIncome(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            label = label.trim(),
            amount = amount,
            iconKey = iconKey,
            year = year,
            month = month,
            createdAt = System.currentTimeMillis(),
            incomeDate = incomeDate
        )
        val entity = income.toEntity(currentUserId)
        dao.upsertExtraIncome(entity)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_incomes")
                    .document(income.id)
                    .set(entity, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing extra income to Firestore", e)
            }
        }
    }

    override suspend fun deleteExtraIncome(id: String) = withContext(ioDispatcher) {
        dao.deleteExtraIncome(id)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_incomes")
                    .document(id)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting extra income from Firestore", e)
            }
        }
    }

    override suspend fun updateExtraIncome(
        id: String,
        label: String,
        amount: Double,
        incomeDate: Long?
    ) = withContext(ioDispatcher) {
        dao.updateExtraIncome(id, label.trim(), amount, incomeDate ?: 0L)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                val updates = mutableMapOf<String, Any>(
                    "label" to label.trim(),
                    "amount" to amount
                )
                if (incomeDate != null) {
                    updates["incomeDate"] = incomeDate
                }
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_incomes")
                    .document(id)
                    .update(updates)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating extra income in Firestore", e)
            }
        }
    }

    override fun getExtraExpenses(year: Int, month: Int): Flow<List<ExtraExpense>> {
        return dao.getExtraExpenses(currentUserId, year, month).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getExtraExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExtraExpense>> {
        return dao.getExtraExpensesBetween(currentUserId, startTime, endTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addExtraExpense(
        label: String,
        amount: Double,
        iconKey: String,
        year: Int,
        month: Int,
        expenseDate: Long
    ) = withContext(ioDispatcher) {
        val expense = ExtraExpense(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            label = label.trim(),
            amount = amount,
            iconKey = iconKey,
            year = year,
            month = month,
            createdAt = System.currentTimeMillis(),
            expenseDate = expenseDate
        )
        val entity = expense.toEntity(currentUserId)
        dao.upsertExtraExpense(entity)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_expenses")
                    .document(expense.id)
                    .set(entity, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing new extra expense to Firestore", e)
            }
        }
    }

    override suspend fun updateExtraExpense(
        id: String,
        label: String,
        amount: Double,
        expenseDate: Long?
    ) = withContext(ioDispatcher) {
        dao.updateExtraExpense(id, label.trim(), amount, expenseDate ?: 0L)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                val updates = mutableMapOf<String, Any>(
                    "label" to label.trim(),
                    "amount" to amount
                )
                if (expenseDate != null) {
                    updates["expenseDate"] = expenseDate
                }
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_expenses")
                    .document(id)
                    .update(updates)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating extra expense in Firestore", e)
            }
        }
    }

    override suspend fun deleteExtraExpense(id: String) = withContext(ioDispatcher) {
        dao.deleteExtraExpense(id)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_expenses")
                    .document(id)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting extra expense in Firestore", e)
            }
        }
    }

    override suspend fun syncFromFirebase(): Unit = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            // Sincronizar perfil financiero
            val profileDoc = firestore.collection("users")
                .document(uid)
                .collection("financial_profile")
                .document("profile")
                .get()
                .await()

            if (profileDoc.exists()) {
                val profileEntity = profileDoc.toObject(FinancialProfileEntity::class.java)
                if (profileEntity != null) {
                    val localProfile = dao.getFinancialProfile(uid).firstOrNull()
                    if (localProfile == null || profileEntity.updatedAt >= localProfile.updatedAt) {
                        dao.upsertFinancialProfile(profileEntity.copy(id = uid))
                    }
                }
            }

            // Sincronizar gastos extra
            val expensesSnap = firestore.collection("users")
                .document(uid)
                .collection("extra_expenses")
                .get()
                .await()

            for (doc in expensesSnap.documents) {
                val expenseEntity = doc.toObject(ExtraExpenseEntity::class.java)
                if (expenseEntity != null) {
                    dao.upsertExtraExpense(expenseEntity.copy(userId = uid))
                }
            }

            // Sincronizar registros mensuales históricos
            val recordsSnap = firestore.collection("users")
                .document(uid)
                .collection("monthly_records")
                .get()
                .await()

            for (doc in recordsSnap.documents) {
                val recordEntity = doc.toObject(com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity::class.java)
                if (recordEntity != null) {
                    dao.upsertMonthlyRecord(recordEntity.copy(userId = uid))
                }
            }

            // Sincronizar ganancias extra
            val incomesSnap = firestore.collection("users")
                .document(uid)
                .collection("extra_incomes")
                .get()
                .await()

            for (doc in incomesSnap.documents) {
                val incomeEntity = doc.toObject(com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity::class.java)
                if (incomeEntity != null) {
                    dao.upsertExtraIncome(incomeEntity.copy(userId = uid))
                }
            }

            // Sincronizar ingresos variables mensuales
            val monthlyIncomesSnap = firestore.collection("users")
                .document(uid)
                .collection("monthly_incomes")
                .get()
                .await()

            for (doc in monthlyIncomesSnap.documents) {
                val mEntity = doc.toObject(com.farbalapps.rinde.data.local.entity.MonthlyIncomeEntity::class.java)
                if (mEntity != null) {
                    dao.upsertMonthlyVariableIncome(mEntity.copy(userId = uid))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing financial data from Firestore", e)
        }
    }

    override fun getMonthlyRecord(year: Int, month: Int): Flow<com.farbalapps.rinde.domain.model.MonthlyFinancialRecord?> {
        return dao.getMonthlyRecord(currentUserId, year, month).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveMonthlyRecord(record: com.farbalapps.rinde.domain.model.MonthlyFinancialRecord) = withContext(ioDispatcher) {
        val entity = record.toEntity(currentUserId)
        dao.upsertMonthlyRecord(entity)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("monthly_records")
                    .document("${record.year}_${record.month}")
                    .set(entity, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing monthly record to Firestore", e)
            }
        }
    }

    override suspend fun checkAndPerformMonthlyRollover() = withContext(ioDispatcher) {
        val cal = java.util.Calendar.getInstance()
        val currentYear = cal.get(java.util.Calendar.YEAR)
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1

        val prevMonth = if (currentMonth == 1) 12 else currentMonth - 1
        val prevYear = if (currentMonth == 1) currentYear - 1 else currentYear

        // Verificar si el mes anterior ya fue consolidado y cerrado
        val existingPrevRecord = dao.getMonthlyRecord(currentUserId, prevYear, prevMonth).firstOrNull()
        if (existingPrevRecord == null || !existingPrevRecord.isClosed) {
            val expensesList = dao.getExtraExpenses(currentUserId, prevYear, prevMonth).firstOrNull() ?: emptyList()
            val extraTotal = expensesList.sumOf { it.amount }
            val profileEntity = dao.getFinancialProfile(currentUserId).firstOrNull()

            val profileStartDate = profileEntity?.customStartDate ?: (profileEntity?.updatedAt ?: 0L)
            val profileCal = java.util.Calendar.getInstance().apply {
                timeInMillis = if (profileStartDate > 0L) profileStartDate else System.currentTimeMillis()
            }
            val pStartYear = profileCal.get(java.util.Calendar.YEAR)
            val pStartMonth = profileCal.get(java.util.Calendar.MONTH) + 1

            val wasProfileActiveInPrevMonth = profileEntity != null && profileEntity.income > 0.0 && (
                pStartYear < prevYear || (pStartYear == prevYear && pStartMonth <= prevMonth)
            )

            val income = if (existingPrevRecord != null && existingPrevRecord.income > 0.0) {
                existingPrevRecord.income
            } else if (wasProfileActiveInPrevMonth) {
                profileEntity?.income ?: 0.0
            } else {
                0.0
            }
            val available = income - extraTotal

            val healthStatus = when {
                income <= 0.0 -> "GOOD"
                available < 0.0 -> "CRITICAL"
                (extraTotal / income) > 0.85 -> "WARNING"
                else -> "GOOD"
            }

            val record = com.farbalapps.rinde.domain.model.MonthlyFinancialRecord(
                id = "${currentUserId}_${prevYear}_${prevMonth}",
                userId = currentUserId,
                year = prevYear,
                month = prevMonth,
                income = income,
                extraExpensesTotal = extraTotal,
                listTotal = existingPrevRecord?.listTotal ?: 0.0,
                goalsCommittedTotal = existingPrevRecord?.goalsCommittedTotal ?: 0.0,
                availableAmount = available - (existingPrevRecord?.listTotal ?: 0.0) - (existingPrevRecord?.goalsCommittedTotal ?: 0.0),
                healthStatus = healthStatus,
                isClosed = true,
                updatedAt = System.currentTimeMillis()
            )
            saveMonthlyRecord(record)
        }
    }
}
