package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.CustomProductHistoryRepository
import javax.inject.Inject

class SaveCustomProductHistoryUseCase @Inject constructor(
    private val repository: CustomProductHistoryRepository
) {
    suspend operator fun invoke(name: String, category: String) {
        if (name.isNotBlank()) {
            repository.saveToHistory(name, category)
        }
    }
}
