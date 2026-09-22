package com.farbalapps.rinde.domain.usecase.dashboard

import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import javax.inject.Inject

/**
 * Caso de uso para coordinar la sincronización de datos del dashboard,
 * metas y listas guardadas, además de ejecutar la verificación mensual de rollover.
 */
class SyncDashboardDataUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val goalsRepository: GoalsRepository,
    private val savedListRepository: SavedListRepository
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        dashboardRepository.syncFromFirebase()
        dashboardRepository.checkAndPerformMonthlyRollover()
        goalsRepository.syncGoals()
        savedListRepository.syncSavedLists()
    }
}
