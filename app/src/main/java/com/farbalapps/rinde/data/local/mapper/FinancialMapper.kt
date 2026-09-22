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
    customEndDate = customEndDate,
    isVariableIncome = isVariableIncome
)

fun FinancialProfile.toEntity(userId: String): FinancialProfileEntity = FinancialProfileEntity(
    id = userId,
    income = income,
    incomeFrequency = incomeFrequency.name,
    currency = currency,
    updatedAt = updatedAt,
    customStartDate = customStartDate,
    customEndDate = customEndDate,
    isVariableIncome = isVariableIncome
)

fun ExtraExpenseEntity.toDomain(): ExtraExpense = ExtraExpense(
    id = id,
    userId = userId,
    label = label,
    amount = amount,
    iconKey = iconKey,
    month = month,
    year = year,
    createdAt = createdAt,
    expenseDate = if (expenseDate > 0) expenseDate else createdAt
)

fun ExtraExpense.toEntity(userId: String): ExtraExpenseEntity = ExtraExpenseEntity(
    id = id,
    userId = userId,
    label = label,
    amount = amount,
    iconKey = iconKey,
    month = month,
    year = year,
    createdAt = createdAt,
    expenseDate = expenseDate
)

fun com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity.toDomain(): com.farbalapps.rinde.domain.model.ExtraIncome =
    com.farbalapps.rinde.domain.model.ExtraIncome(
        id = id,
        userId = userId,
        label = label,
        amount = amount,
        iconKey = iconKey,
        month = month,
        year = year,
        createdAt = createdAt,
        incomeDate = if (incomeDate > 0) incomeDate else createdAt
    )

fun com.farbalapps.rinde.domain.model.ExtraIncome.toEntity(userId: String): com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity =
    com.farbalapps.rinde.data.local.entity.ExtraIncomeEntity(
        id = id,
        userId = userId,
        label = label,
        amount = amount,
        iconKey = iconKey,
        month = month,
        year = year,
        createdAt = createdAt,
        incomeDate = incomeDate
    )

fun com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity.toDomain(): com.farbalapps.rinde.domain.model.MonthlyFinancialRecord =
    com.farbalapps.rinde.domain.model.MonthlyFinancialRecord(
        id = id,
        userId = userId,
        year = year,
        month = month,
        income = income,
        extraExpensesTotal = extraExpensesTotal,
        listTotal = listTotal,
        goalsCommittedTotal = goalsCommittedTotal,
        availableAmount = availableAmount,
        healthStatus = healthStatus,
        isClosed = isClosed,
        updatedAt = updatedAt
    )

fun com.farbalapps.rinde.domain.model.MonthlyFinancialRecord.toEntity(userId: String): com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity =
    com.farbalapps.rinde.data.local.entity.MonthlyFinancialRecordEntity(
        id = if (id.isNotBlank()) id else "${userId}_${year}_${month}",
        userId = userId,
        year = year,
        month = month,
        income = income,
        extraExpensesTotal = extraExpensesTotal,
        listTotal = listTotal,
        goalsCommittedTotal = goalsCommittedTotal,
        availableAmount = availableAmount,
        healthStatus = healthStatus,
        isClosed = isClosed,
        updatedAt = updatedAt
    )

