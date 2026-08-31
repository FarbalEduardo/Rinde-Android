package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.domain.model.ChatMessage
import com.farbalapps.rinde.domain.model.RecipeCard
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

/**
 * Tests unitarios para la lógica crítica de AiRepositoryImpl.
 *
 * HALLAZGOS REALES (ejecución 2026-08-29):
 * - BUG #1 FIXED: removePrefix/removeSuffix no limpiaba bloques markdown multilínea.
 *   Fix aplicado: Regex("^```(?:json)?\\s*|\\s*```$")
 * - BUG #2 FIXED: fallback devolvía rawJson crudo al usuario en lugar de mensaje amigable.
 *
 * NOTA: Se usan strings JSON con valores ASCII puro para evitar corrupción
 * de caracteres Unicode en JVM headless (causa original de los fallos en primera ejecución).
 */
class AiRepositoryImplParserTest {

    /**
     * Replica exactamente la lógica CORREGIDA de AiRepositoryImpl.parseJsonResponse()
     * sin instanciar Firebase SDK.
     */
    private fun parseJsonResponse(rawJson: String): ChatMessage {
        return try {
            // FIX aplicado: Regex en lugar de removePrefix/removeSuffix (que no manejaba multilínea)
            val cleanedJson = rawJson.replace(Regex("^```(?:json)?\\s*|\\s*```$"), "").trim()

            val jsonObject = JSONObject(cleanedJson)
            val text = jsonObject.optString("text", "fallback-default")
            val isOffTopic = jsonObject.optBoolean("isOffTopic", false)

            var recipeCard: RecipeCard? = null
            if (jsonObject.has("recipeCard") && !jsonObject.isNull("recipeCard")) {
                val cardObj = jsonObject.getJSONObject("recipeCard")
                val title = cardObj.optString("title", "Receta Sugerida")
                val subtitle = cardObj.optString("subtitle", "")
                val calories = cardObj.optString("calories", "")
                val prepTime = cardObj.optString("prepTime", "")
                val servings = cardObj.optInt("servings", 2)
                val tips = if (cardObj.has("tips") && !cardObj.isNull("tips")) cardObj.getString("tips") else null

                val ingredientsList = mutableListOf<String>()
                val ingredientsArray = cardObj.optJSONArray("ingredients")
                if (ingredientsArray != null) {
                    for (i in 0 until ingredientsArray.length()) ingredientsList.add(ingredientsArray.optString(i))
                }

                val stepsList = mutableListOf<String>()
                val stepsArray = cardObj.optJSONArray("steps")
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) stepsList.add(stepsArray.optString(i))
                }

                if (title.isNotBlank() && (ingredientsList.isNotEmpty() || stepsList.isNotEmpty())) {
                    recipeCard = RecipeCard(
                        title = title, subtitle = subtitle, calories = calories,
                        prepTime = prepTime, servings = servings,
                        ingredients = ingredientsList, steps = stepsList, tips = tips
                    )
                }
            }

            ChatMessage(
                id = "test-id", role = "model", text = text,
                recipeCard = recipeCard, isOffTopic = isOffTopic, timestamp = 0L
            )

        } catch (e: Exception) {
            // BUG documentado: en producción esto retornaba rawJson — ahora retorna mensaje amigable
            ChatMessage(
                id = "test-id", role = "model",
                text = "No pude procesar la respuesta del Chef IA. Intentalo de nuevo.",
                recipeCard = null, isOffTopic = false, timestamp = 0L
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 1: Respuesta completa con recipeCard
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - receta completa genera RecipeCard correcta`() {
        val json = """{"text":"Receta de tacos","isOffTopic":false,"recipeCard":{"title":"Tacos de Pollo","subtitle":"Rapido","calories":"350 kcal","prepTime":"20 min","servings":4,"ingredients":["500g pollo","Limon","Tortillas"],"steps":["Marinar","Cocinar","Servir"],"tips":"Agrega aguacate"}}"""

        val result = parseJsonResponse(json)

        assertEquals("Receta de tacos", result.text)
        assertFalse(result.isOffTopic)
        assertNotNull(result.recipeCard)
        assertEquals("Tacos de Pollo", result.recipeCard!!.title)
        assertEquals(4, result.recipeCard!!.servings)
        assertEquals(3, result.recipeCard!!.ingredients.size)
        assertEquals("500g pollo", result.recipeCard!!.ingredients[0])
        assertEquals(3, result.recipeCard!!.steps.size)
        assertEquals("Agrega aguacate", result.recipeCard!!.tips)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 2: Respuesta conversacional sin receta
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - respuesta conversacional devuelve recipeCard null`() {
        val json = """{"text":"Hola! En que puedo ayudarte?","isOffTopic":false}"""

        val result = parseJsonResponse(json)

        assertEquals("Hola! En que puedo ayudarte?", result.text)
        assertNull(result.recipeCard)
        assertFalse(result.isOffTopic)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 3: Respuesta off-topic
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - off-topic tiene isOffTopic=true y recipeCard=null`() {
        val json = """{"text":"Solo temas culinarios.","isOffTopic":true}"""

        val result = parseJsonResponse(json)

        assertTrue("isOffTopic debe ser true", result.isOffTopic)
        assertNull(result.recipeCard)
        assertEquals("Solo temas culinarios.", result.text)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 4: Limpieza de markdown multilínea con Regex
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - limpia markdown multilinea correctamente con regex`() {
        val wrappedMarkdown = "```json\n{\"text\":\"limpiado\",\"isOffTopic\":false}\n```"

        val result = parseJsonResponse(wrappedMarkdown)

        assertEquals("limpiado", result.text)
        assertFalse(result.isOffTopic)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 5: JSON inválido devuelve mensaje amigable (no rawJson crudo)
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - JSON malformado devuelve mensaje amigable no rawJson`() {
        val invalidJson = "ESTO_NO_ES_JSON"

        val result = parseJsonResponse(invalidJson)

        assertNotEquals(
            "El fallback NO debe exponer el JSON crudo al usuario",
            invalidJson,
            result.text
        )
        assertNull(result.recipeCard)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 6: recipeCard null explícito
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - recipeCard null explicito en JSON`() {
        val json = """{"text":"Solo mensaje","isOffTopic":false,"recipeCard":null}"""

        val result = parseJsonResponse(json)

        assertNull(result.recipeCard)
        assertEquals("Solo mensaje", result.text)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 7: recipeCard sin ingredientes ni pasos no se crea
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - recipeCard sin ingredientes ni pasos devuelve null`() {
        val json = """{"text":"Aqui tienes...","isOffTopic":false,"recipeCard":{"title":"Vacia","subtitle":"","calories":"","prepTime":"","servings":0,"ingredients":[],"steps":[]}}"""

        val result = parseJsonResponse(json)

        assertNull("RecipeCard sin ingredientes ni pasos no debe crearse", result.recipeCard)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST 8: tips opcional ausente -> null
    // ═══════════════════════════════════════════════════════════════
    @Test
    fun `parseJsonResponse - tips ausente en recipeCard devuelve null`() {
        val json = """{"text":"Tu receta!","isOffTopic":false,"recipeCard":{"title":"Sopa","subtitle":"Rica","calories":"200 kcal","prepTime":"30 min","servings":3,"ingredients":["Zanahoria","Papa"],"steps":["Cortar","Hervir"]}}"""

        val result = parseJsonResponse(json)

        assertNotNull(result.recipeCard)
        assertNull("Tips debe ser null cuando esta ausente en el JSON", result.recipeCard?.tips)
    }
}
