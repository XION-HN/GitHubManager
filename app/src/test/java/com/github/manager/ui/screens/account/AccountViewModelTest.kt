package com.github.manager.ui.screens.account

import app.cash.turbine.test
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import com.github.manager.ui.i18n.LanguageMode
import com.github.manager.ui.i18n.ThemeMode
import com.github.manager.ui.i18n.languageModeState
import com.github.manager.ui.i18n.themeModeState
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
class AccountViewModelTest {

    private val gitHubRepository: GitHubRepository = mockk()

    private val tokenManager: TokenManager = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = 1, login = "testuser", name = "Test User",
        bio = "A developer", publicRepos = 10, followers = 50, following = 10,
        createdAt = "2020-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.success(testUser)
        coEvery { tokenManager.loadLanguageMode() } returns null
        coEvery { tokenManager.loadThemeMode() } returns null

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

        coVerify { tokenManager.loadLanguageMode() }
        coVerify { tokenManager.loadThemeMode() }
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
        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.failure(RuntimeException("Network error"))

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
    fun `loadProfile sets loading state then completes`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)

        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertNotNull(finalState.user)
    }

    @Test
    fun `logout clears all data`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        coVerify { tokenManager.clearAll() }
    }

    @Test
    fun `switchAccount clears old data and saves new token`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.success(testUser)

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.switchAccount("ghp_new_token")
        advanceUntilIdle()

        coVerify { tokenManager.clearAll() }
        coVerify { tokenManager.saveToken("ghp_new_token") }
        coVerify(atLeast = 2) { gitHubRepository.getAuthenticatedUser() }
    }

    @Test
    fun `saveLanguageMode persists to tokenManager`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveLanguageMode("CHINESE")
        advanceUntilIdle()

        coVerify { tokenManager.saveLanguageMode("CHINESE") }
    }

    @Test
    fun `saveThemeMode persists to tokenManager`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveThemeMode("DARK")
        advanceUntilIdle()

        coVerify { tokenManager.saveThemeMode("DARK") }
    }

    @Test
    fun `loadPreferences applies Chinese language mode`() = runTest(testDispatcher) {
        coEvery { tokenManager.loadLanguageMode() } returns "CHINESE"
        coEvery { tokenManager.loadThemeMode() } returns null

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(LanguageMode.CHINESE, languageModeState.value)
    }

    @Test
    fun `loadPreferences applies English language mode`() = runTest(testDispatcher) {
        coEvery { tokenManager.loadLanguageMode() } returns "ENGLISH"

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(LanguageMode.ENGLISH, languageModeState.value)
    }

    @Test
    fun `loadPreferences defaults to Bilingual for unknown language`() = runTest(testDispatcher) {
        coEvery { tokenManager.loadLanguageMode() } returns "UNKNOWN"

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(LanguageMode.BILINGUAL, languageModeState.value)
    }

    @Test
    fun `loadPreferences applies Light theme mode`() = runTest(testDispatcher) {
        coEvery { tokenManager.loadThemeMode() } returns "LIGHT"

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, themeModeState.value)
    }

    @Test
    fun `loadPreferences applies Dark theme mode`() = runTest(testDispatcher) {
        coEvery { tokenManager.loadThemeMode() } returns "DARK"

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, themeModeState.value)
    }

    @Test
    fun `loadPreferences defaults to System for unknown theme`() = runTest(testDispatcher) {
        coEvery { tokenManager.loadThemeMode() } returns "UNKNOWN"

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, themeModeState.value)
    }

    @Test
    fun `switchAccount clears old data and saves new token`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.success(testUser)

        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.switchAccount("ghp_new_token")
        advanceUntilIdle()

        coVerify { tokenManager.clearAll() }
        coVerify { tokenManager.saveToken("ghp_new_token") }
        coVerify(atLeast = 2) { gitHubRepository.getAuthenticatedUser() }
    }

    @Test
    fun `loadProfile failure clears user`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.failure(RuntimeException("API Error"))

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
    fun `saveLanguageMode persists to tokenManager`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveLanguageMode("ENGLISH")
        advanceUntilIdle()

        coVerify { tokenManager.saveLanguageMode("ENGLISH") }
    }

    @Test
    fun `saveThemeMode persists to tokenManager`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.saveThemeMode("DARK")
        advanceUntilIdle()

        coVerify { tokenManager.saveThemeMode("DARK") }
    }
}
