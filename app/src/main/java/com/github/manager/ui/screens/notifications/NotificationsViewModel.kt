package com.github.manager.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.model.Notification
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val perPage = 50

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            currentPage = 1
            gitHubRepository.getNotifications(page = currentPage)
                .onSuccess { notifications ->
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isLoading = false,
                        hasMore = notifications.size >= perPage
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return
        viewModelScope.launch {
            currentPage++
            gitHubRepository.getNotifications(page = currentPage)
                .onSuccess { notifications ->
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications + notifications,
                        hasMore = notifications.size >= perPage
                    )
                }
                .onFailure {
                    currentPage--
                }
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            gitHubRepository.markNotificationRead(id)
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.map {
                    if (it.id == id) it.copy(unread = false) else it
                }
            )
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            gitHubRepository.markAllNotificationsRead()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications.map { it.copy(unread = false) }
                    )
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            currentPage = 1
            gitHubRepository.getNotifications(page = 1)
                .onSuccess { notifications ->
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isRefreshing = false,
                        hasMore = notifications.size >= perPage
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isRefreshing = false)
                }
        }
    }
}
