package com.farbalapps.rinde.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialPeriodTest {

    @Test
    fun testMonthPeriodDurationAndIncome() {
        val monthPeriod = FinancialPeriod.Month(year = 2026, month = 9) // Septiembre has 30 days
        assertEquals(30, monthPeriod.durationDays)

        val profile = FinancialProfile(
            income = 30000.0,
            incomeFrequency = IncomeFrequency.MONTHLY
        )

        val periodIncome = monthPeriod.calculatePeriodIncome(profile)
        assertEquals(30000.0, periodIncome, 0.001)

        val prevMonth = monthPeriod.previous() as FinancialPeriod.Month
        assertEquals(8, prevMonth.month)
        assertEquals(2026, prevMonth.year)

        val nextMonth = monthPeriod.next() as FinancialPeriod.Month
        assertEquals(10, nextMonth.month)
        assertEquals(2026, nextMonth.year)
    }

    @Test
    fun testFortnightPeriodDurationAndIncome() {
        // 1ra Quincena Septiembre 2026
        val firstHalf = FinancialPeriod.Fortnight(year = 2026, month = 9, isFirstHalf = true)
        assertEquals(15, firstHalf.durationDays)

        val profile = FinancialProfile(
            income = 15000.0,
            incomeFrequency = IncomeFrequency.BIWEEKLY
        )

        // If user is BIWEEKLY, their direct income for 1 fortnight is their configured income ($15,000)
        val income = firstHalf.calculatePeriodIncome(profile)
        assertEquals(15000.0, income, 0.001)

        val secondHalf = firstHalf.next() as FinancialPeriod.Fortnight
        assertEquals(false, secondHalf.isFirstHalf)
        assertEquals(15, secondHalf.durationDays) // Sep has 30 days, 30 - 15 = 15
    }

    @Test
    fun testDayPeriodDurationAndIncome() {
        val dayPeriod = FinancialPeriod.Day(dateMillis = 1757078400000L) // Some fixed day
        assertEquals(1, dayPeriod.durationDays)

        val profile = FinancialProfile(
            income = 30000.0,
            incomeFrequency = IncomeFrequency.MONTHLY
        )
        // Daily rate for $30,000/month = $1,000/day
        val dayIncome = dayPeriod.calculatePeriodIncome(profile)
        assertEquals(1000.0, dayIncome, 0.001)
    }

    @Test
    fun testCustomPeriodDurationAndIncome() {
        val startMillis = 1000000000L
        val tenDaysMillis = startMillis + (9L * 24 * 60 * 60 * 1000) // 10 days inclusive
        val customPeriod = FinancialPeriod.Custom(startMillis, tenDaysMillis)

        assertEquals(10, customPeriod.durationDays)

        val profile = FinancialProfile(
            income = 30000.0,
            incomeFrequency = IncomeFrequency.MONTHLY
        )
        // Daily rate $1,000 * 10 days = $10,000
        val customIncome = customPeriod.calculatePeriodIncome(profile)
        assertEquals(10000.0, customIncome, 0.001)
    }

    @Test
    fun testFinancialProfileCustomFrequency() {
        val start = 0L
        val end = 14L * 24 * 60 * 60 * 1000 // 15 days
        val profile = FinancialProfile(
            income = 15000.0,
            incomeFrequency = IncomeFrequency.CUSTOM,
            customStartDate = start,
            customEndDate = end
        )

        assertEquals(15, profile.customPeriodDays)
        // $15,000 for 15 days = $1,000/day -> monthly equivalent = $30,000
        assertEquals(30000.0, profile.monthlyEquivalent, 0.001)
        assertEquals(1000.0, profile.dailyIncome, 0.001)
    }
}
