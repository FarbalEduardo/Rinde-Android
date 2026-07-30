package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.CustomProductHistoryRepository
import javax.inject.Inject

class DeleteCustomProductHistoryUseCase @Inject constructor(
    private val repository: CustomProductHistoryRepository
) {
    suspend operator fun invoke(name: String) {
        repository.deleteFromHistory(name)
    }
}
