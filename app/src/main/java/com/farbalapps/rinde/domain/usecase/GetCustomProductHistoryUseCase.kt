package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.CustomProductHistory
import com.farbalapps.rinde.domain.repository.CustomProductHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCustomProductHistoryUseCase @Inject constructor(
    private val repository: CustomProductHistoryRepository
) {
    operator fun invoke(): Flow<List<CustomProductHistory>> {
        return repository.getHistory()
    }
}
