package com.github.manager.ui.screens.account

import app.cash.turbine.test
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import com.github.manager.ui.i18n.LanguageMode
import com.github.manager.ui.i18n.ThemeMode
import com.github.manager.ui.i18n.languageModeState
import com.github.manager.ui.i18n.themeModeState
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
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @Mock
    private lateinit var gitHubRepository: GitHubRepository

    @Mock
    private lateinit var tokenManager: TokenManager

    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = 1, login = "testuser", name = "Test User",
        bio = "A developer", publicRepos = 10, followers = 50, following = 10,
        createdAt = "2020-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.success(testUser))
        whenever(tokenManager.loadLanguageMode()).thenReturn(null)
        whenever(tokenManager.loadThemeMode()).thenReturn(null)

        languageModeState.value = LanguageMode.BILINGUAL
        themeModeState.value = ThemeMode.SYSTEM
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        languageModeState.value = LanguageMode.BILINGUAL
        themeModeState.value = ThemeMode.SYSTEM
    }

    @Test
    fun `init loads profile and preferences`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("testuser", state.user?.login)
            assertFalse(state.isLoading)
        }

        verify(tokenManager).loadLanguageMode()
        verify(tokenManager).loadThemeMode()
    }

    @Test
    fun `loadProfile success sets user`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.user)
            assertEquals("Test User", state.user?.name)
            assertEquals(10, state.user?.publicRepos)
            assertEquals(50, state.user?.followers)
        }
    }

    @Test
    fun `loadProfile failure sets error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.failure(RuntimeException("Network error")))

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.user)
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `loadProfile sets loading state`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)

        viewModel.loadProfile()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
        }

        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `logout clears all data`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        verify(tokenManager).clearAll()
    }

    @Test
    fun `switchAccount clears old data and saves new token`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.success(testUser))

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.switchAccount("ghp_new_token")
        advanceUntilIdle()

        verify(tokenManager).clearAll()
        verify(tokenManager).saveToken("ghp_new_token")
        verify(gitHubRepository, atLeast(2)).getAuthenticatedUser()
    }

    @Test
    fun `saveLanguageMode persists to tokenManager`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveLanguageMode("CHINESE")
        advanceUntilIdle()

        verify(tokenManager).saveLanguageMode("CHINESE")
    }

    @Test
    fun `saveThemeMode persists to tokenManager`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveThemeMode("DARK")
        advanceUntilIdle()

        verify(tokenManager).saveThemeMode("DARK")
    }

    @Test
    fun `loadPreferences applies Chinese language mode`() = runTest(testDispatcher) {
        whenever(tokenManager.loadLanguageMode()).thenReturn("CHINESE")
        whenever(tokenManager.loadThemeMode()).thenReturn(null)

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(LanguageMode.CHINESE, languageModeState.value)
    }

    @Test
    fun `loadPreferences applies English language mode`() = runTest(testDispatcher) {
        whenever(tokenManager.loadLanguageMode()).thenReturn("ENGLISH")

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(LanguageMode.ENGLISH, languageModeState.value)
    }

    @Test
    fun `loadPreferences defaults to Bilingual for unknown language`() = runTest(testDispatcher) {
        whenever(tokenManager.loadLanguageMode()).thenReturn("UNKNOWN")

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(LanguageMode.BILINGUAL, languageModeState.value)
    }

    @Test
    fun `loadPreferences applies Light theme mode`() = runTest(testDispatcher) {
        whenever(tokenManager.loadThemeMode()).thenReturn("LIGHT")

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, themeModeState.value)
    }

    @Test
    fun `loadPreferences applies Dark theme mode`() = runTest(testDispatcher) {
        whenever(tokenManager.loadThemeMode()).thenReturn("DARK")

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, themeModeState.value)
    }

    @Test
    fun `loadPreferences defaults to System for unknown theme`() = runTest(testDispatcher) {
        whenever(tokenManager.loadThemeMode()).thenReturn("UNKNOWN")

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, themeModeState.value)
    }

    @Test
    fun `switchAccount failure sets error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.success(testUser))
        whenever(tokenManager.clearAll()).thenThrow(RuntimeException("DataStore error"))

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.switchAccount("ghp_bad_token")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `loadProfile failure clears user`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.failure(RuntimeException("API Error")))

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.user)
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `saveLanguageMode updates global state`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveLanguageMode("ENGLISH")
        advanceUntilIdle()

        assertEquals(LanguageMode.ENGLISH, languageModeState.value)
        verify(tokenManager).saveLanguageMode("ENGLISH")
    }

    @Test
    fun `saveThemeMode updates global state`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveThemeMode("DARK")
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, themeModeState.value)
        verify(tokenManager).saveThemeMode("DARK")
    }
}
