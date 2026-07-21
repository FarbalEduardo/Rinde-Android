package com.farbalapps.rinde.ui.screen.home.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.usecase.goals.GoalsSummary

sealed interface GoalsUiState {
    object Loading : GoalsUiState
    
    object Empty : GoalsUiState
    
    data class Content(
        val featuredGoal: SavingsGoal?,
        val secondaryGoals: List<SavingsGoal>,
        val summary: GoalsSummary,
        val canAddMore: Boolean,
        val isOffline: Boolean = false
    ) : GoalsUiState

    data class Error(val message: String) : GoalsUiState
}

sealed interface GoalsEvent {
    data class GoalCompleted(val title: String) : GoalsEvent
    data class DepositExceedsTarget(val goalId: String, val amount: Double, val excess: Double, val note: String) : GoalsEvent
    data class ValidationError(val message: String) : GoalsEvent
    object GoalLimitReached : GoalsEvent
    data class Success(val message: String) : GoalsEvent
}
