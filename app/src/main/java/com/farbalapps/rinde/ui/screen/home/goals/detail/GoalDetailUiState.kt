package com.farbalapps.rinde.ui.screen.home.goals.detail

import com.farbalapps.rinde.domain.model.GoalTransaction
import com.farbalapps.rinde.domain.model.SavingsGoal

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState
    data class Error(val message: String) : GoalDetailUiState
    data class Content(
        val goal: SavingsGoal,
        val transactions: List<GoalTransaction> = emptyList(),
        val categoryName: String,
        val formattedCreationDate: String,
        val formattedLimitDate: String,
        val formattedMonthlyContribution: String,
        val formattedLastContribution: String
    ) : GoalDetailUiState
}
