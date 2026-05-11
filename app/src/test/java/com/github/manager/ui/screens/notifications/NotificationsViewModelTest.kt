package com.github.manager.ui.screens.notifications

import com.github.manager.data.model.Notification
import com.github.manager.data.model.NotificationRepo
import com.github.manager.data.model.NotificationSubject
import com.github.manager.data.model.Owner
import com.github.manager.data.repository.GitHubRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val gitHubRepository: GitHubRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private val testNotifications = listOf(
        Notification(
            id = 1,
            unread = true,
            reason = "subscribe",
            subject = NotificationSubject(title = "Test Issue", type = "Issue", url = ""),
            repository = NotificationRepo(id = 1, name = "repo", fullName = "owner/repo", owner = Owner(id = 1, login = "owner"))
        ),
        Notification(
            id = 2,
            unread = false,
            reason = "mention",
            subject = NotificationSubject(title = "Test PR", type = "PullRequest", url = ""),
            repository = NotificationRepo(id = 2, name = "repo2", fullName = "owner/repo2", owner = Owner(id = 1, login = "owner"))
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadNotifications success loads notifications`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getNotifications(page = 1) } returns Result.success(testNotifications)

        val viewModel = NotificationsViewModel(gitHubRepository)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.notifications.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadNotifications failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getNotifications(page = 1) } returns Result.failure(RuntimeException("Network error"))

        val viewModel = NotificationsViewModel(gitHubRepository)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `markAsRead updates notification unread status`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getNotifications(page = 1) } returns Result.success(testNotifications)
        coEvery { gitHubRepository.markNotificationRead(1) } returns Result.success(true)

        val viewModel = NotificationsViewModel(gitHubRepository)
        advanceUntilIdle()

        viewModel.markAsRead(1)
        advanceUntilIdle()

        val notification = viewModel.uiState.value.notifications.first { it.id == 1L }
        assertFalse(notification.unread)
        coVerify { gitHubRepository.markNotificationRead(1) }
    }

    @Test
    fun `markAllAsRead marks all notifications as read`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getNotifications(page = 1) } returns Result.success(testNotifications)
        coEvery { gitHubRepository.markAllNotificationsRead() } returns Result.success(true)

        val viewModel = NotificationsViewModel(gitHubRepository)
        advanceUntilIdle()

        viewModel.markAllAsRead()
        advanceUntilIdle()

        viewModel.uiState.value.notifications.forEach {
            assertFalse(it.unread)
        }
        coVerify { gitHubRepository.markAllNotificationsRead() }
    }

    @Test
    fun `refresh reloads notifications`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getNotifications(page = 1) } returns Result.success(testNotifications)

        val viewModel = NotificationsViewModel(gitHubRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(atLeast = 2) { gitHubRepository.getNotifications(page = 1) }
    }
}
