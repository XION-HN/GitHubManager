package com.github.manager.ui.screens.auth

import app.cash.turbine.test
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @Mock
    private lateinit var tokenManager: TokenManager

    @Mock
    private lateinit var gitHubRepository: GitHubRepository

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(tokenManager.token).thenReturn(flowOf(null))

        val user = User(id = 1, login = "testuser", name = "Test User")
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.success(user))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AuthViewModel {
        viewModel = AuthViewModel(tokenManager, gitHubRepository)
        return viewModel
    }

    @Test
    fun `init with saved token validates automatically`() = runTest(testDispatcher) {
        whenever(tokenManager.token).thenReturn(flowOf("ghp_saved_token"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
        }
    }

    @Test
    fun `init without saved token does not auto authenticate`() = runTest(testDispatcher) {
        whenever(tokenManager.token).thenReturn(flowOf(null))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
        }
    }

    @Test
    fun `onTokenChanged updates token and clears error`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("new_token")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("new_token", state.token)
            assertNull(state.error)
        }
    }

    @Test
    fun `login with blank token shows error`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("")
        viewModel.login()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.isAuthenticated)
        }
    }

    @Test
    fun `login with whitespace-only token shows error`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("   ")
        viewModel.login()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `login success authenticates user`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("ghp_valid_token")
        viewModel.login()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

        verify(tokenManager).saveToken("ghp_valid_token")
    }

    @Test
    fun `login failure shows error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.failure(RuntimeException("Invalid token")))

        createViewModel()

        viewModel.onTokenChanged("ghp_invalid_token")
        viewModel.login()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertNotNull(state.error)
            assertTrue(state.error?.contains("Authentication failed") == true || state.error?.contains("认证失败") == true)
        }
    }

    @Test
    fun `logout clears state`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.logout()
        advanceUntilIdle()

        verify(tokenManager).clearAll()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertEquals("", state.token)
            assertNull(state.error)
        }
    }

    @Test
    fun `checkAuth returns isAuthenticated value`() = runTest(testDispatcher) {
        createViewModel()

        assertFalse(viewModel.checkAuth())

        viewModel.onTokenChanged("ghp_token")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.checkAuth())
    }

    @Test
    fun `login saves token before validation`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("ghp_test")
        viewModel.login()
        advanceUntilIdle()

        verify(tokenManager).saveToken("ghp_test")
        verify(gitHubRepository).getAuthenticatedUser()
    }

    @Test
    fun `login sets loading state during request`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("ghp_token")
        viewModel.login()

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
        }

        advanceUntilIdle()

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
        }
    }

    @Test
    fun `init with invalid saved token sets error`() = runTest(testDispatcher) {
        whenever(tokenManager.token).thenReturn(flowOf("ghp_invalid_token"))
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.failure(RuntimeException("Invalid token")))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
        }
    }

    @Test
    fun `login with null token shows error`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("")
        viewModel.login()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.isAuthenticated)
        }
    }

    @Test
    fun `logout resets isLoading to false`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `onTokenChanged clears previous error`() = runTest(testDispatcher) {
        createViewModel()

        viewModel.onTokenChanged("")
        viewModel.login()

        viewModel.onTokenChanged("ghp_new")
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }
}
