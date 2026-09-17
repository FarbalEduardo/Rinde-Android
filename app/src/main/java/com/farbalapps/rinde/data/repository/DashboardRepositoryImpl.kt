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
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing financial data from Firestore", e)
        }
    }
}
