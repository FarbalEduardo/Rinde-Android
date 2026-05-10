package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.dao.CustomProductHistoryDao
import com.farbalapps.rinde.data.local.entity.CustomProductHistoryEntity
import com.farbalapps.rinde.domain.model.CustomProductHistory
import com.farbalapps.rinde.domain.repository.CustomProductHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomProductHistoryRepositoryImpl @Inject constructor(
    private val dao: CustomProductHistoryDao
) : CustomProductHistoryRepository {
    override fun getHistory(): Flow<List<CustomProductHistory>> {
        return dao.getHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveToHistory(name: String, category: String) {
        dao.insert(CustomProductHistoryEntity(name = name, category = category))
    }

    override suspend fun deleteFromHistory(name: String) {
        dao.delete(name)
    }

    private fun CustomProductHistoryEntity.toDomain() = CustomProductHistory(
        name = name,
        category = category,
        addedAt = addedAt
    )
}
