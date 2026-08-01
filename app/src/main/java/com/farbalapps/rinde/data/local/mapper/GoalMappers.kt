package com.farbalapps.rinde.data.local.mapper

import com.farbalapps.rinde.data.local.entity.SavingsGoalEntity
import com.farbalapps.rinde.data.local.entity.GoalTransactionEntity
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.model.GoalTransaction

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    userId = userId,
    title = title,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    iconKey = iconKey,
    colorKey = colorKey,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    monthlySnapshotAmount = monthlySnapshotAmount,
    targetDate = targetDate
)

fun SavingsGoal.toEntity(isSynced: Boolean = false) = SavingsGoalEntity(
    id = id,
    userId = userId,
    title = title,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    iconKey = iconKey,
    colorKey = colorKey,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    monthlySnapshotAmount = monthlySnapshotAmount,
    targetDate = targetDate,
    isSynced = isSynced
)

fun GoalTransactionEntity.toDomain() = GoalTransaction(
    id = id,
    goalId = goalId,
    amount = amount,
    note = note,
    timestamp = timestamp
)

fun GoalTransaction.toEntity(isSynced: Boolean = false) = GoalTransactionEntity(
    id = id,
    goalId = goalId,
    amount = amount,
    note = note,
    timestamp = timestamp,
    isSynced = isSynced
)
