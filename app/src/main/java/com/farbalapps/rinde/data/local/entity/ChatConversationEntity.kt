package com.farbalapps.rinde.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farbalapps.rinde.domain.model.ChatConversation
import com.farbalapps.rinde.domain.model.ChatMessage

/**
 * Room entity representing a Chef IA conversation session stored locally.
 */
@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceListName: String?,
    val messages: List<ChatMessage>
)

fun ChatConversationEntity.toDomain(): ChatConversation {
    return ChatConversation(
        id = id,
        userId = userId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceListName = sourceListName,
        messages = messages
    )
}

fun ChatConversation.toEntity(): ChatConversationEntity {
    return ChatConversationEntity(
        id = id,
        userId = userId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceListName = sourceListName,
        messages = messages
    )
}
