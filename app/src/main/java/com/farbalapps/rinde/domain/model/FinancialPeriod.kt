package com.farbalapps.rinde.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Representa los diferentes tipos de granularidad temporal soportados en el detalle financiero.
 */
enum class PeriodType {
    MONTH,
    FORTNIGHT,
    WEEK,
    DAY,
    CUSTOM
}

/**
 * Modelo de dominio sellado que define un periodo financiero seleccionado por el usuario.
 */
sealed class FinancialPeriod {
    abstract val type: PeriodType
    abstract val displayName: String
    abstract val startTimestamp: Long
    abstract val endTimestamp: Long
    abstract val durationDays: Int

    /**
     * Calcula el ingreso proporcional o directo que le corresponde al usuario en este periodo.
     */
    fun calculatePeriodIncome(profile: FinancialProfile?): Double {
        if (profile == null || profile.income <= 0.0) return 0.0
        return when (this) {
            is Month -> {
                if (profile.incomeFrequency == IncomeFrequency.MONTHLY) profile.income
                else profile.dailyIncome * durationDays
            }
            is Fortnight -> {
                if (profile.incomeFrequency == IncomeFrequency.BIWEEKLY) profile.income
                else profile.dailyIncome * durationDays
            }
            is Week -> {
                if (profile.incomeFrequency == IncomeFrequency.WEEKLY) profile.income
                else profile.dailyIncome * durationDays
            }
            is Day -> {
                if (profile.incomeFrequency == IncomeFrequency.DAILY) profile.income
                else profile.dailyIncome * durationDays
            }
            is Custom -> {
                profile.dailyIncome * durationDays
            }
        }
    }

    abstract fun previous(): FinancialPeriod
    abstract fun next(): FinancialPeriod

    /**
     * Periodo mensual completo (ej. Septiembre 2026).
     */
    data class Month(
        val year: Int,
        val month: Int // 1-12
    ) : FinancialPeriod() {
        override val type: PeriodType = PeriodType.MONTH

        override val startTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        override val endTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis
        }

        override val durationDays: Int = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        override val displayName: String = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val formatted = fmt.format(cal.time)
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        override fun previous(): FinancialPeriod {
            return if (month == 1) Month(year - 1, 12) else Month(year, month - 1)
        }

        override fun next(): FinancialPeriod {
            return if (month == 12) Month(year + 1, 1) else Month(year, month + 1)
        }
    }

    /**
     * Periodo quincenal (1ra Quincena: 1-15, 2da Quincena: 16-fin de mes).
     */
    data class Fortnight(
        val year: Int,
        val month: Int,
        val isFirstHalf: Boolean
    ) : FinancialPeriod() {
        override val type: PeriodType = PeriodType.FORTNIGHT

        private val maxDay: Int = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        override val startTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, if (isFirstHalf) 1 else 16)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        override val endTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, if (isFirstHalf) 15 else maxDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis
        }

        override val durationDays: Int = if (isFirstHalf) 15 else (maxDay - 15)

        override val displayName: String = run {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }
            val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            if (isFirstHalf) "1ra Quincena (1-15 $monthName $year)"
            else "2da Quincena (16-$maxDay $monthName $year)"
        }

        override fun previous(): FinancialPeriod {
            return if (isFirstHalf) {
                val (prevYear, prevMonth) = if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)
                Fortnight(prevYear, prevMonth, isFirstHalf = false)
            } else {
                Fortnight(year, month, isFirstHalf = true)
            }
        }

        override fun next(): FinancialPeriod {
            return if (isFirstHalf) {
                Fortnight(year, month, isFirstHalf = false)
            } else {
                val (nextYear, nextMonth) = if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)
                Fortnight(nextYear, nextMonth, isFirstHalf = true)
            }
        }
    }

    /**
     * Periodo semanal (7 días exactos).
     */
    data class Week(
        val startMillis: Long
    ) : FinancialPeriod() {
        override val type: PeriodType = PeriodType.WEEK

        override val startTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                timeInMillis = startMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        override val endTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis
        }

        override val durationDays: Int = 7

        override val displayName: String = run {
            val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
            val startStr = fmt.format(Date(startTimestamp))
            val endStr = fmt.format(Date(endTimestamp))
            "Semana: $startStr - $endStr"
        }

        override fun previous(): FinancialPeriod {
            val cal = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
                add(Calendar.DAY_OF_YEAR, -7)
            }
            return Week(cal.timeInMillis)
        }

        override fun next(): FinancialPeriod {
            val cal = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
                add(Calendar.DAY_OF_YEAR, 7)
            }
            return Week(cal.timeInMillis)
        }
    }

    /**
     * Periodo de un solo día.
     */
    data class Day(
        val dateMillis: Long
    ) : FinancialPeriod() {
        override val type: PeriodType = PeriodType.DAY

        override val startTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        override val endTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis
        }

        override val durationDays: Int = 1

        override val displayName: String = run {
            val todayCal = Calendar.getInstance()
            val targetCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val isToday = todayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    todayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

            val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val formatted = fmt.format(targetCal.time)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            if (isToday) "Hoy · $formatted" else formatted
        }

        override fun previous(): FinancialPeriod {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }
            return Day(cal.timeInMillis)
        }

        override fun next(): FinancialPeriod {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            return Day(cal.timeInMillis)
        }
    }

    /**
     * Periodo personalizado elegido con selector de fechas / calendario.
     */
    data class Custom(
        val startMillis: Long,
        val endMillis: Long
    ) : FinancialPeriod() {
        override val type: PeriodType = PeriodType.CUSTOM

        override val startTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                timeInMillis = kotlin.math.min(startMillis, endMillis)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }

        override val endTimestamp: Long = run {
            val cal = Calendar.getInstance().apply {
                timeInMillis = kotlin.math.max(startMillis, endMillis)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis
        }

        override val durationDays: Int = run {
            val diff = endTimestamp - startTimestamp
            kotlin.math.max(1, (diff / (1000L * 60 * 60 * 24)).toInt() + 1)
        }

        override val displayName: String = run {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val startStr = fmt.format(Date(startTimestamp))
            val endStr = fmt.format(Date(endTimestamp))
            "$startStr - $endStr ($durationDays días)"
        }

        override fun previous(): FinancialPeriod {
            val span = (durationDays * 24L * 60 * 60 * 1000)
            return Custom(startTimestamp - span, endTimestamp - span)
        }

        override fun next(): FinancialPeriod {
            val span = (durationDays * 24L * 60 * 60 * 1000)
            return Custom(startTimestamp + span, endTimestamp + span)
        }
    }

    companion object {
        fun currentMonth(): Month {
            val cal = Calendar.getInstance()
            return Month(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }

        fun currentFortnight(): Fortnight {
            val cal = Calendar.getInstance()
            val day = cal.get(Calendar.DAY_OF_MONTH)
            return Fortnight(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, day <= 15)
        }

        fun currentWeek(): Week {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            }
            return Week(cal.timeInMillis)
        }

        fun today(): Day {
            return Day(System.currentTimeMillis())
        }
    }
}
