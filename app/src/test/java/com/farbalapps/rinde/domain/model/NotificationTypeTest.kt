package com.farbalapps.rinde.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el modelo de notificaciones [AppNotification] y tipos [NotificationType].
 * Valida la inclusión y comportamiento del tipo NEW_REPLY para HU-02.
 */
class NotificationTypeTest {

    @Test
    fun `NotificationType contiene tipo NEW_REPLY`() {
        val types = NotificationType.values().map { it.name }
        assertTrue(types.contains("NEW_REPLY"))
        assertTrue(types.contains("NEW_COMMENT"))
        assertTrue(types.contains("POST_EXPIRED"))
        assertTrue(types.contains("POST_VERIFIED"))
    }

    @Test
    fun `AppNotification con tipo NEW_REPLY almacena datos de respuesta correctamente`() {
        val notification = AppNotification(
            id = "notif_reply_123",
            type = NotificationType.NEW_REPLY,
            postId = "post_abc",
            postTitle = "Descuento en Supermercado",
            actorName = "Carlos Mendoza",
            actorPhotoUrl = "https://example.com/carlos.jpg",
            timestamp = 1700000000000L,
            isRead = false
        )

        assertEquals("notif_reply_123", notification.id)
        assertEquals(NotificationType.NEW_REPLY, notification.type)
        assertEquals("post_abc", notification.postId)
        assertEquals("Descuento en Supermercado", notification.postTitle)
        assertEquals("Carlos Mendoza", notification.actorName)
        assertEquals("https://example.com/carlos.jpg", notification.actorPhotoUrl)
        assertEquals(1700000000000L, notification.timestamp)
        assertEquals(false, notification.isRead)
    }

    @Test
    fun `NotificationType valueOf mapea string NEW_REPLY de Firestore sin excepciones`() {
        val parsedType = NotificationType.valueOf("NEW_REPLY")
        assertEquals(NotificationType.NEW_REPLY, parsedType)
    }
}
