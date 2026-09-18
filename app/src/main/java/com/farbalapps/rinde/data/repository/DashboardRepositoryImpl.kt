package com.farbalapps.rinde.data.repository

import android.util.Log
import com.farbalapps.rinde.data.local.dao.FinancialDao
import com.farbalapps.rinde.data.local.entity.ExtraExpenseEntity
import com.farbalapps.rinde.data.local.entity.FinancialProfileEntity
import com.farbalapps.rinde.data.local.mapper.toDomain
import com.farbalapps.rinde.data.local.mapper.toEntity
import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.domain.model.ExtraExpense
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
        private const val TAG = "DashboardRepository"
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
        customEndDate: Long?
    ) = withContext(ioDispatcher) {
        val profile = FinancialProfile(
            id = currentUserId,
            income = income,
            incomeFrequency = frequency,
            currency = currency,
            updatedAt = System.currentTimeMillis(),
            customStartDate = customStartDate,
            customEndDate = customEndDate
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
        month: Int
    ) = withContext(ioDispatcher) {
        val expense = ExtraExpense(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            label = label.trim(),
            amount = amount,
            iconKey = iconKey,
            year = year,
            month = month,
            createdAt = System.currentTimeMillis()
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
        amount: Double
    ) = withContext(ioDispatcher) {
        dao.updateExtraExpense(id, label.trim(), amount)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("extra_expenses")
                    .document(id)
                    .update(
                        mapOf(
                            "label" to label.trim(),
                            "amount" to amount
                        )
                    )
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
                    dao.upsertFinancialProfile(profileEntity.copy(id = uid))
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
            val income = profileEntity?.income ?: 0.0
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
