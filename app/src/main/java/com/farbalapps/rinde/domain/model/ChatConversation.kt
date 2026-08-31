package com.farbalapps.rinde.domain.model

/**
 * Domain model representing a Chef IA chat conversation session.
 * Pure Kotlin data class with zero Android framework dependencies.
 *
 * @param id Unique identifier for the conversation session.
 * @param userId Firebase UID of the owner.
 * @param title Title of the session (e.g. recipe name or first prompt summary).
 * @param createdAt Timestamp when created.
 * @param updatedAt Timestamp when last message was added.
 * @param sourceListName Name of the shopping list active during this chat.
 * @param messages List of messages in this session.
 */
data class ChatConversation(
    val id: String = "",
    val userId: String = "",
    val title: String = "Nueva Receta",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceListName: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

/**
 * Single chat message within a session.
 */
data class ChatMessage(
    val id: String = "",
    val role: String = "user", // "user" | "model"
    val text: String = "",
    val recipeCard: RecipeCard? = null,
    val isOffTopic: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Structured recipe recommendation card details from Gemini.
 */
data class RecipeCard(
    val title: String = "",
    val subtitle: String = "",
    val calories: String = "",
    val prepTime: String = "",
    val servings: Int = 1,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val tips: String? = null
)
