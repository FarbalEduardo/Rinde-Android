package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getUnreadCount(userId: String): Flow<Int>
    fun getNotifications(userId: String): Flow<List<AppNotification>>
    suspend fun sendNotification(targetUserId: String, notification: AppNotification): Result<Unit>
    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit>
    suspend fun markAllAsRead(userId: String): Result<Unit>
}
