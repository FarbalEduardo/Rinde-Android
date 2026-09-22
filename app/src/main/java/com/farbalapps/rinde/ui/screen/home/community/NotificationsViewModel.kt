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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

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

    private val _userIdFlow = MutableStateFlow(auth.currentUser?.uid.orEmpty())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotificationsUiState> = _userIdFlow.flatMapLatest { uid ->
        if (uid.isEmpty()) {
            flowOf(NotificationsUiState(isLoading = false, currentUserId = ""))
        } else {
            combine(
                notificationRepository.getUnreadCount(uid),
                notificationRepository.getNotifications(uid)
            ) { unreadCount, notifications ->
                NotificationsUiState(
                    unreadCount = unreadCount,
                    notifications = notifications,
                    isLoading = false,
                    currentUserId = uid
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState(isLoading = true, currentUserId = auth.currentUser?.uid.orEmpty())
    )

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val newUid = firebaseAuth.currentUser?.uid.orEmpty()
            if (_userIdFlow.value != newUid) {
                _userIdFlow.value = newUid
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = _userIdFlow.value
        if (uid.isEmpty()) return
        viewModelScope.launch {
            notificationRepository.markAsRead(uid, notificationId)
        }
    }

    fun markAllAsRead() {
        val uid = _userIdFlow.value
        if (uid.isEmpty()) return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(uid)
        }
    }
}
