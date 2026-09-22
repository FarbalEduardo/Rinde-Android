package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.ChatConversation
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Chef IA Chat Conversation repository.
 * Handles local Room storage and Firebase Firestore synchronization.
 */
interface ChatRepository {
    /**
     * Observe saved chat conversations for the specified user (up to max limit).
     */
    fun getConversations(userId: String): Flow<List<ChatConversation>>

    /**
     * Get a specific conversation by ID.
     */
    suspend fun getConversationById(id: String): ChatConversation?

    /**
     * Save or update a conversation session.
     * Enforces the maximum 10-conversation limit automatically.
     */
    suspend fun saveConversation(conversation: ChatConversation): Result<Unit>

    /**
     * Delete a conversation by ID.
     */
    suspend fun deleteConversation(id: String): Result<Unit>
}
