package com.farbalapps.rinde.ui.screen.home.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.AppNotification
import com.farbalapps.rinde.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val unreadCount: Int = 0,
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val currentUserId: String = ""
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.getUnreadCount(currentUserId),
        notificationRepository.getNotifications(currentUserId)
    ) { unreadCount, notifications ->
        NotificationsUiState(
            unreadCount = unreadCount,
            notifications = notifications,
            isLoading = false,
            currentUserId = currentUserId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState(isLoading = true, currentUserId = currentUserId)
    )

    fun markAsRead(notificationId: String) {
        val uid = currentUserId
        if (uid.isEmpty()) return
        viewModelScope.launch {
            notificationRepository.markAsRead(uid, notificationId)
        }
    }

    fun markAllAsRead() {
        val uid = currentUserId
        if (uid.isEmpty()) return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(uid)
        }
    }
}
