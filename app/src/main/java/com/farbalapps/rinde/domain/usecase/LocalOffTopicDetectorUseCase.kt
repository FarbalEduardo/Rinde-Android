package com.farbalapps.rinde.domain.usecase

import javax.inject.Inject

/**
 * Use case to detect non-culinary questions locally BEFORE calling the Gemini API.
 * This saves 100% of API tokens for queries completely unrelated to food/cooking/recipes.
 */
class LocalOffTopicDetectorUseCase @Inject constructor() {

    private val offTopicKeywords = setOf(
        "llanta", "llantas", "carro", "carros", "auto", "autos", "coche", "automóvil",
        "motor", "aceite de motor", "gasolina", "freno", "frenos", "mecánico", "mecanico",
        "programar", "código", "codigo", "programación", "python", "java", "kotlin",
        "fútbol", "futbol", "partido", "gol", "champions", "messi", "ronaldo",
        "política", "politica", "elecciones", "presidente", "gobernador",
        "matemáticas", "matematicas", "álgebra", "fisica", "química", "tarea escolar",
        "fontanero", "plomería", "electricidad", "cable", "pantalla", "celular", "laptop"
    )

    /**
     * Returns true if the query is clearly off-topic and unrelated to food/cooking.
     */
    operator fun invoke(userText: String): Boolean {
        val lower = userText.trim().lowercase()
        return offTopicKeywords.any { keyword -> lower.contains(keyword) }
    }
}
