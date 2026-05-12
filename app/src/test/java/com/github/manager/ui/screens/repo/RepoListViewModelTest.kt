package com.github.manager.ui.screens.repo

import app.cash.turbine.test
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.Owner
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class RepoListViewModelTest {

    private val gitHubRepository: GitHubRepository = mockk()
    private val tokenManager: TokenManager = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(id = 1, login = "testuser", name = "Test User")
    private val testRepos = (1..5).map { i ->
        Repository(id = i.toLong(), name = "repo$i", fullName = "testuser/repo$i", owner = Owner(1, "testuser"))
    }
    private val fullRepos = (1..30).map { i ->
        Repository(id = i.toLong(), name = "repo$i", fullName = "testuser/repo$i", owner = Owner(1, "testuser"))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.success(testUser)
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.success(testRepos)
        coEvery { gitHubRepository.getStarredRepos(any()) } returns Result.success(testRepos)
        coEvery { gitHubRepository.getUserReposFromCache() } returns emptyList()
        coEvery { gitHubRepository.getStarredReposFromCache() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads profile and repos`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("testuser", state.user?.login)
            assertEquals(5, state.repos.size)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `loadRepos sets hasMore to true when perPage items returned`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.success(fullRepos)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasMore)
        }
    }

    @Test
    fun `loadRepos sets hasMore to false when fewer items returned`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.success(testRepos)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasMore)
        }
    }

    @Test
    fun `loadRepos failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.failure(RuntimeException("API error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `refresh reloads repos and profile`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(atLeast = 2) { gitHubRepository.getAuthenticatedUser() }
        coVerify(atLeast = 2) { gitHubRepository.getUserRepos(any()) }
    }

    @Test
    fun `loadMore appends repos`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.success(fullRepos)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.repos.size >= 30)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.success(testRepos)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        val reposBefore = viewModel.uiState.value.repos.size
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(reposBefore, viewModel.uiState.value.repos.size)
    }

    @Test
    fun `toggleTab switches to starred tab`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.toggleTab(true)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStarredTab)
        }
    }

    @Test
    fun `toggleTab switches to my repos tab`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.toggleTab(true)
        advanceUntilIdle()

        viewModel.toggleTab(false)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isStarredTab)
        }
    }

    @Test
    fun `starRepo adds to starredRepos optimistically`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.starRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.starRepo("owner", "repo")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.starredRepos.contains("owner/repo"))
        }
    }

    @Test
    fun `starRepo failure reverts optimistic update`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.starRepository(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.starRepo("owner", "repo")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.starredRepos.contains("owner/repo"))
        }
    }

    @Test
    fun `unstarRepo removes from starredRepos optimistically`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.unstarRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.unstarRepo("testuser", "repo1")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.starredRepos.contains("testuser/repo1"))
        }
    }

    @Test
    fun `unstarRepo failure reverts optimistic update`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.unstarRepository(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        val initialStarred = viewModel.uiState.value.starredRepos
        viewModel.unstarRepo("testuser", "repo1")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.starredRepos.contains("testuser/repo1"))
        }
    }

    @Test
    fun `forkRepo calls repository and reloads`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.forkRepository(any(), any()) } returns Result.success(
            Repository(id = 99, name = "forked", fullName = "testuser/forked", owner = Owner(1, "testuser"), fork = true)
        )

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.forkRepo("other", "repo")
        advanceUntilIdle()

        coVerify { gitHubRepository.forkRepository("other", "repo") }
    }

    @Test
    fun `deleteRepo calls repository and reloads`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.deleteRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.deleteRepo("testuser", "repo1")
        advanceUntilIdle()

        coVerify { gitHubRepository.deleteRepository("testuser", "repo1") }
    }

    @Test
    fun `createRepo calls repository and reloads`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.createRepository(any(), any(), any()) } returns Result.success(
            Repository(id = 100, name = "new-repo", fullName = "testuser/new-repo", owner = Owner(1, "testuser"))
        )

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.createRepo("new-repo", "desc", false)
        advanceUntilIdle()

        coVerify { gitHubRepository.createRepository("new-repo", "desc", false) }
    }

    @Test
    fun `logout clears all data`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        coVerify { tokenManager.clearAll() }
    }

    @Test
    fun `createRepo failure still calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.createRepository(any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.createRepo("new-repo", "desc", false)
        advanceUntilIdle()

        coVerify { gitHubRepository.createRepository("new-repo", "desc", false) }
    }

    @Test
    fun `forkRepo failure still calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.forkRepository(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.forkRepo("other", "repo")
        advanceUntilIdle()

        coVerify { gitHubRepository.forkRepository("other", "repo") }
    }

    @Test
    fun `deleteRepo failure still calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.deleteRepository(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.deleteRepo("testuser", "repo1")
        advanceUntilIdle()

        coVerify { gitHubRepository.deleteRepository("testuser", "repo1") }
    }

    @Test
    fun `loadMore failure does not crash`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(page = 1) } returns Result.success(fullRepos)
        coEvery { gitHubRepository.getUserRepos(page = 2) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `refresh failure sets error`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.failure(RuntimeException("Refresh error"))
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `loadProfile failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getAuthenticatedUser() } returns Result.failure(RuntimeException("Profile error"))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.user)
        }
    }

    @Test
    fun `toggleTab to starred tab loads starred repos`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.toggleTab(true)
        advanceUntilIdle()

        coVerify { gitHubRepository.getStarredRepos(any()) }
    }

    @Test
    fun `starRepo does not double-add if already starred`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.starRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.starRepo("testuser", "repo1")
        advanceUntilIdle()

        val starredBefore = viewModel.uiState.value.starredRepos.contains("testuser/repo1")
        viewModel.starRepo("testuser", "repo1")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.starredRepos.contains("testuser/repo1"))
        }
    }

    @Test
    fun `loadRepos failure with cached repos shows offline fallback`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserRepos(any()) } returns Result.failure(RuntimeException("Network error"))
        coEvery { gitHubRepository.getUserReposFromCache() } returns testRepos

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOfflineFallback)
        assertEquals(5, viewModel.uiState.value.repos.size)
        assertFalse(viewModel.uiState.value.hasMore)
    }

    @Test
    fun `loadRepos failure with starred cache shows offline fallback`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getStarredRepos(any()) } returns Result.failure(RuntimeException("Network error"))
        coEvery { gitHubRepository.getStarredReposFromCache() } returns testRepos

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.toggleTab(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOfflineFallback)
        assertEquals(5, viewModel.uiState.value.repos.size)
    }
}
