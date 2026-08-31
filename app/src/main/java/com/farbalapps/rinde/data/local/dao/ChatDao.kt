package com.farbalapps.rinde.data.local.dao

import androidx.room.*
import com.farbalapps.rinde.data.local.entity.ChatConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Chef IA conversation sessions.
 * Manages Room queries and enforces the 10-conversation limit.
 */
@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_conversations WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 10")
    fun getConversationsFlow(userId: String): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 10")
    suspend fun getConversations(userId: String): List<ChatConversationEntity>

    @Query("SELECT * FROM chat_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ChatConversationEntity)

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Deletes oldest conversations for the user if total count exceeds 10.
     */
    @Query("DELETE FROM chat_conversations WHERE userId = :userId AND id NOT IN (SELECT id FROM chat_conversations WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 10)")
    suspend fun trimOldConversations(userId: String)
}
