package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.ChatMessage

/**
 * Domain repository interface for AI Chef interactions.
 * Pure Kotlin contract adhering to Clean Architecture principles.
 */
interface AiRepository {
    /**
     * Generates a culinary AI response based on previous conversation history
     * and optional user-selected pantry/shopping list ingredients.
     *
     * @param history The previous messages in the current conversation turn.
     * @param userPrompt The current prompt text sent by the user.
     * @param selectedIngredients List of selected ingredient names (max 5).
     * @return [Result] containing the hydrated [ChatMessage] with structured [com.farbalapps.rinde.domain.model.RecipeCard] if applicable.
     */
    suspend fun generateRecipeResponse(
        history: List<ChatMessage>,
        userPrompt: String,
        selectedIngredients: List<String>
    ): Result<ChatMessage>
}
