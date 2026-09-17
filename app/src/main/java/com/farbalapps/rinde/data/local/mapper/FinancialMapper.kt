package com.farbalapps.rinde.data.local.mapper

import com.farbalapps.rinde.data.local.entity.ExtraExpenseEntity
import com.farbalapps.rinde.data.local.entity.FinancialProfileEntity
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency

fun FinancialProfileEntity.toDomain(): FinancialProfile = FinancialProfile(
    id = id,
    income = income,
    incomeFrequency = IncomeFrequency.fromString(incomeFrequency),
    currency = currency,
    updatedAt = updatedAt,
    customStartDate = customStartDate,
    customEndDate = customEndDate
)

fun FinancialProfile.toEntity(userId: String): FinancialProfileEntity = FinancialProfileEntity(
    id = userId,
    income = income,
    incomeFrequency = incomeFrequency.name,
    currency = currency,
    updatedAt = updatedAt,
    customStartDate = customStartDate,
    customEndDate = customEndDate
)

fun ExtraExpenseEntity.toDomain(): ExtraExpense = ExtraExpense(
    id = id,
    userId = userId,
    label = label,
    amount = amount,
    iconKey = iconKey,
    month = month,
    year = year,
    createdAt = createdAt
)

fun ExtraExpense.toEntity(userId: String): ExtraExpenseEntity = ExtraExpenseEntity(
    id = id,
    userId = userId,
    label = label,
    amount = amount,
    iconKey = iconKey,
    month = month,
    year = year,
    createdAt = createdAt
)
