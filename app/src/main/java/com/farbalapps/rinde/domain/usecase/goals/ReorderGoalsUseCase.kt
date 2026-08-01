package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

class ReorderGoalsUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke(goals: List<SavingsGoal>): Result<Unit> {
        return runCatching {
            repository.reorderGoals(goals)
        }
    }
}
