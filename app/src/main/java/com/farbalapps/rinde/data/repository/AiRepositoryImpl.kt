package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.domain.model.ChatMessage
import com.farbalapps.rinde.domain.model.RecipeCard
import com.farbalapps.rinde.domain.repository.AiRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AiRepository] using Firebase AI Logic SDK with Google AI (Gemini 2.0 Flash).
 * Supports App Check in production (via Play Integrity) and debug token verification in development.
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AiRepository {

    private val systemPrompt = """
        Eres Chef IA de Rinde 👨‍🍳✨, un asistente culinario experto en cocina práctica, económica y deliciosa.
        Tu misión es crear y recomendar recetas aprovechando al máximo los ingredientes que el usuario tiene disponibles en su despensa o lista de compras.
        
        REGLAS DE ORO:
        1. Si el usuario te proporciona ingredientes o pide una receta, diseña una receta fácil, sabrosa y bien explicada.
        2. Siempre que sugieras una receta, DEBES incluir el objeto 'recipeCard' completo con title, subtitle, calories, prepTime, servings, ingredients y steps.
        3. Si el usuario solo saluda o hace una consulta conversacional sobre técnicas de cocina, responde cordialmente en el campo 'text' y puedes dejar 'recipeCard' como null.
        4. Si el usuario pregunta sobre temas ajenos a comida, cocina, recetas o nutrición, responde amablemente que solo atiendes temas culinarios, marca 'isOffTopic': true y 'recipeCard': null.
        5. SIEMPRE responde en español latinoamericano claro y accesible.
        6. Devuelve SIEMPRE y ÚNICAMENTE el JSON estructurado según el esquema solicitado.
    """.trimIndent()

    private val recipeResponseSchema = Schema.obj(
        mapOf(
            "text" to Schema.string("Mensaje conversacional del Chef para el chat"),
            "isOffTopic" to Schema.boolean("True si el usuario preguntó algo ajeno a cocina/comida"),
            "recipeCard" to Schema.obj(
                mapOf(
                    "title" to Schema.string("Título descriptivo y apetitoso del platillo"),
                    "subtitle" to Schema.string("Subtítulo breve (ej. 'Económico y nutritivo' o 'Listo en 20 min')"),
                    "calories" to Schema.string("Estimación calórica por porción (ej. '~350 kcal')"),
                    "prepTime" to Schema.string("Tiempo estimado de preparación (ej. '25 min')"),
                    "servings" to Schema.integer("Número de porciones"),
                    "ingredients" to Schema.array(Schema.string("Ingrediente con cantidad recomendada")),
                    "steps" to Schema.array(Schema.string("Paso de preparación claro y conciso")),
                    "tips" to Schema.string("Consejo del Chef para mejorar el platillo o ahorrar")
                ),
                optionalProperties = listOf("tips")
            )
        ),
        optionalProperties = listOf("recipeCard")
    )

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-3.6-flash",
                systemInstruction = content { text(systemPrompt) },
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = recipeResponseSchema
                    temperature = 0.7f
                }
            )
    }

    companion object {
        private const val TAG = "ChefAI_AiRepository"
    }

    override suspend fun generateRecipeResponse(
        history: List<ChatMessage>,
        userPrompt: String,
        selectedIngredients: List<String>
    ): Result<ChatMessage> = withContext(ioDispatcher) {
        val promptBuilder = StringBuilder()
        if (selectedIngredients.isNotEmpty()) {
            promptBuilder.append("Ingredientes disponibles en mi despensa: ")
            promptBuilder.append(selectedIngredients.joinToString(", "))
            promptBuilder.append(".\n\n")
        }
        if (userPrompt.isNotBlank()) {
            promptBuilder.append(userPrompt)
        } else if (selectedIngredients.isNotEmpty()) {
            promptBuilder.append("Recomiéndame la mejor receta que pueda preparar con estos ingredientes.")
        } else {
            promptBuilder.append("¿Qué receta fácil me sugieres para cocinar hoy?")
        }

        val finalPrompt = promptBuilder.toString()

        try {
            android.util.Log.d(
                TAG,
                "🚀 [CHEF AI] Enviando mensaje a Gemini 3.6 Flash. PromptLength=${finalPrompt.length}, HistoryCount=${history.size}, Ingredientes=${selectedIngredients.size}"
            )

            // Convert previous messages to Firebase AI Content history (skipping the initial greeting and the last prompt)
            val historyContents = history
                .filter { it.text.isNotBlank() }
                .takeLast(10) // Limit context turns to keep prompt focused and latency low
                .map { msg ->
                    content(role = if (msg.role == "user") "user" else "model") {
                        text(msg.text)
                    }
                }

            val chatSession = generativeModel.startChat(history = historyContents)
            val response = chatSession.sendMessage(finalPrompt)

            val rawResponseText = response.text.orEmpty().trim()
            if (rawResponseText.isEmpty()) {
                val emptyEx = IllegalStateException("Respuesta vacía del Chef IA")
                logDetailedError(emptyEx, finalPrompt, selectedIngredients)
                return@withContext Result.failure(emptyEx)
            }

            android.util.Log.d(TAG, "✅ [CHEF AI] Respuesta recibida con éxito (${rawResponseText.length} caracteres)")
            val parsedMessage = parseJsonResponse(rawResponseText)
            Result.success(parsedMessage)
        } catch (e: Exception) {
            logDetailedError(e, finalPrompt, selectedIngredients)
            Result.failure(e)
        }
    }

    private fun logDetailedError(e: Throwable, finalPrompt: String, selectedIngredients: List<String>) {
        val errorMsg = e.message.orEmpty()
        val exceptionClassName = e.javaClass.canonicalName ?: e.javaClass.name

        val (category, diagnosticHint) = when {
            exceptionClassName.contains("FirebaseAppCheck", ignoreCase = true) ||
            errorMsg.contains("AppCheck", ignoreCase = true) ||
            errorMsg.contains("403", ignoreCase = true) ||
            errorMsg.contains("401", ignoreCase = true) ||
            errorMsg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            errorMsg.contains("API_KEY_INVALID", ignoreCase = true) ||
            errorMsg.contains("Invalid API key", ignoreCase = true) -> {
                "AUTENTICACIÓN / APP CHECK / API KEY (401/403)" to
                    "Verifica los tokens de depuración en Firebase App Check (Debug Provider), la clave de API o los permisos de Firebase Vertex AI en Firebase Console."
            }

            exceptionClassName.contains("QuotaExceeded", ignoreCase = true) ||
            exceptionClassName.contains("TooManyRequests", ignoreCase = true) ||
            errorMsg.contains("429", ignoreCase = true) ||
            errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            errorMsg.contains("Quota exceeded", ignoreCase = true) -> {
                "LÍMITE DE CUOTA / RATE LIMIT EXCEDIDO (429)" to
                    "Se ha alcanzado el límite de peticiones por minuto (RPM) o cuota diaria de la API de Gemini Flash."
            }

            exceptionClassName.contains("UnknownHost", ignoreCase = true) ||
            exceptionClassName.contains("SocketTimeout", ignoreCase = true) ||
            exceptionClassName.contains("FirebaseNetwork", ignoreCase = true) ||
            errorMsg.contains("Unable to resolve host", ignoreCase = true) ||
            errorMsg.contains("timeout", ignoreCase = true) ||
            errorMsg.contains("Failed to connect", ignoreCase = true) -> {
                "ERROR DE RED / CONECTIVIDAD / TIMEOUT" to
                    "El dispositivo no tiene conexión estable a Internet o el servidor de Firebase tardó demasiado en responder."
            }

            exceptionClassName.contains("PromptBlocked", ignoreCase = true) ||
            exceptionClassName.contains("ResponseStopped", ignoreCase = true) ||
            errorMsg.contains("SAFETY", ignoreCase = true) ||
            errorMsg.contains("BLOCKED", ignoreCase = true) ||
            errorMsg.contains("BlockedReason", ignoreCase = true) -> {
                "FILTRO DE SEGURIDAD / CONTENIDO BLOQUEADO" to
                    "El prompt o la respuesta generada fue bloqueada por los filtros de seguridad/política de Google AI."
            }

            exceptionClassName.contains("UnsupportedUserLocation", ignoreCase = true) ||
            errorMsg.contains("User location", ignoreCase = true) ||
            errorMsg.contains("not supported", ignoreCase = true) -> {
                "UBICACIÓN GEOGRÁFICA NO SOPORTADA" to
                    "La API de Gemini o el backend de Firebase AI no está habilitado para la región del usuario o IP actual."
            }

            exceptionClassName.contains("ServerException", ignoreCase = true) ||
            errorMsg.contains("500", ignoreCase = true) ||
            errorMsg.contains("503", ignoreCase = true) ||
            errorMsg.contains("UNAVAILABLE", ignoreCase = true) ||
            errorMsg.contains("DEADLINE_EXCEEDED", ignoreCase = true) -> {
                "ERROR DE SERVIDOR FIREBASE / BACKEND (500/503)" to
                    "Falla interna o sobrecarga momentánea en los servidores de Firebase AI Logic."
            }

            e is org.json.JSONException || exceptionClassName.contains("Serialization", ignoreCase = true) -> {
                "ERROR DE PARSEO / DESERIALIZACIÓN JSON" to
                    "La respuesta de la IA no se pudo convertir al formato esperado (Schema RecipeCard)."
            }

            e is IllegalStateException && errorMsg.contains("vacía", ignoreCase = true) -> {
                "RESPUESTA VACÍA" to
                    "El modelo de IA devolvió una respuesta de texto nula o en blanco."
            }

            else -> {
                "ERROR GENÉRICO / DESCONOCIDO" to
                    "Revisar el stacktrace para más detalles técnicos."
            }
        }

        android.util.Log.e(
            TAG,
            """
            |════════════════════════════════════════════════════════════════════
            |🚨 [CHEF AI - ERROR AL ENVIAR MENSAJE]
            |📌 Categoría: $category
            |🏷️ Excepción: $exceptionClassName
            |💬 Mensaje: $errorMsg
            |🔍 Causa: ${e.cause?.javaClass?.name ?: "Ninguna"} -> ${e.cause?.message ?: "N/A"}
            |💡 Diagnóstico / Sugerencia: $diagnosticHint
            |📥 Prompt: "$finalPrompt"
            |🥕 Ingredientes (${selectedIngredients.size}): [${selectedIngredients.joinToString(", ")}]
            |════════════════════════════════════════════════════════════════════
            """.trimMargin(),
            e
        )
    }

    private fun parseJsonResponse(rawJson: String): ChatMessage {
        return try {
            // Remove markdown code blocks if the model wrapped output in ```json ... ```
            // FIX: Regex cubre el caso multilínea (```json\n{...}\n```) que removePrefix no manejaba.
            val cleanedJson = rawJson.replace(Regex("^```(?:json)?\\s*|\\s*```$"), "").trim()

            val jsonObject = JSONObject(cleanedJson)
            val text = jsonObject.optString("text", "¡Aquí tienes una recomendación especial para ti! 👨‍🍳")
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
                    for (i in 0 until ingredientsArray.length()) {
                        ingredientsList.add(ingredientsArray.optString(i))
                    }
                }

                val stepsList = mutableListOf<String>()
                val stepsArray = cardObj.optJSONArray("steps")
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        stepsList.add(stepsArray.optString(i))
                    }
                }

                if (title.isNotBlank() && (ingredientsList.isNotEmpty() || stepsList.isNotEmpty())) {
                    recipeCard = RecipeCard(
                        title = title,
                        subtitle = subtitle,
                        calories = calories,
                        prepTime = prepTime,
                        servings = servings,
                        ingredients = ingredientsList,
                        steps = stepsList,
                        tips = tips
                    )
                }
            }

            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "model",
                text = text,
                recipeCard = recipeCard,
                isOffTopic = isOffTopic,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            android.util.Log.w("AiRepositoryImpl", "Fallo al deserializar JSON estructurado de Gemini. Raw: $rawJson", e)
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "model",
                text = "No pude procesar la respuesta del Chef IA. Inténtalo de nuevo. 🍳",
                recipeCard = null,
                isOffTopic = false,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
