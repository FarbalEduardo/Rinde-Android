package com.farbalapps.rinde.domain.model

enum class PostVerdict {
    VALIDATING,      // < umbral de votos → gris, "En validación"
    MOSTLY_TRUE,     // ≥ 60% Real → verde/primary, "85% Real" ✓
    MOSTLY_FALSE,    // ≥ 60% Falso → rojo/error, "80% Falso" ✗
    DISPUTED         // Entre 40-60% → gris neutro, "Dudoso" o "Opiniones divididas"
}

object VerdictCalculator {
    const val MIN_VOTES_THRESHOLD = 5
    const val MAJORITY_PERCENT = 0.60f

    fun calculate(truthCount: Int, falseCount: Int): PostVerdict {
        val total = truthCount + falseCount
        if (total < MIN_VOTES_THRESHOLD) return PostVerdict.VALIDATING
        val truthRatio = truthCount.toFloat() / total
        return when {
            truthRatio >= MAJORITY_PERCENT -> PostVerdict.MOSTLY_TRUE
            truthRatio <= (1f - MAJORITY_PERCENT) -> PostVerdict.MOSTLY_FALSE
            else -> PostVerdict.DISPUTED
        }
    }

    fun truthPercent(truthCount: Int, falseCount: Int): Int {
        val total = truthCount + falseCount
        if (total == 0) return 0
        return kotlin.math.round((truthCount.toFloat() / total) * 100).toInt()
    }
}
