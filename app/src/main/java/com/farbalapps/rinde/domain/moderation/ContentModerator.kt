package com.farbalapps.rinde.domain.moderation

/**
 * Moderador Inteligente de Contenido.
 * Encargado de auditar que las publicaciones cumplan con las reglas de la comunidad.
 */
class ContentModerator {

    sealed class ModerationResult {
        object Allowed : ModerationResult()
        data class Rejected(val reason: String) : ModerationResult()
    }

    private val forbiddenKeywords = listOf(
        "perro", "perrito", "gato", "gatito", "cachorro", "animales", "mascota en venta",
        "droga", "marihuana", "cocaína", "arma", "pistola", "balas",
        "sexo", "sexual", "escort", "adulto only", "porno"
    )

    private val forbiddenPatterns = listOf(
        Regex("(?i)venda?\\s+de?\\s+animales"),
        Regex("(?i)sustancias?\\s+il[ií]citas"),
        Regex("(?i)servicios?\\s+sexuales")
    )

    /**
     * Analiza el texto para detectar contenido prohibido.
     */
    fun analyzeText(title: String, description: String): ModerationResult {
        val fullContent = "$title $description".lowercase()

        // 1. Verificación por palabras clave
        for (keyword in forbiddenKeywords) {
            if (fullContent.contains(keyword)) {
                return ModerationResult.Rejected(getExplanation(keyword))
            }
        }

        // 2. Verificación por patrones Regex (más inteligente)
        for (pattern in forbiddenPatterns) {
            if (pattern.containsMatchIn(fullContent)) {
                return ModerationResult.Rejected("El contenido parece infringir nuestras políticas de seguridad.")
            }
        }

        return ModerationResult.Allowed
    }

    private fun getExplanation(keyword: String): String {
        return when {
            keyword.contains("perro") || keyword.contains("animal") || 
            keyword.contains("cachorro") || keyword.contains("gato") -> 
                "Lo sentimos, pero nuestra comunidad prohíbe la venta de animales para asegurar su bienestar. Puedes publicar accesorios o comida para mascotas."
            keyword.contains("droga") || keyword.contains("arma") -> 
                "Este tipo de productos está prohibido por seguridad legal."
            keyword.contains("sex") || keyword.contains("adulto") -> 
                "No permitimos contenido de naturaleza sexual o para adultos."
            else -> "Tu publicación contiene términos que infringen nuestras políticas de comunidad."
        }
    }
}
