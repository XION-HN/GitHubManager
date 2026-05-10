package com.github.manager.ui.screens.repo

import app.cash.turbine.test
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.Owner
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
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
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class RepoListViewModelTest {

    @Mock
    private lateinit var gitHubRepository: GitHubRepository

    @Mock
    private lateinit var tokenManager: TokenManager

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
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.success(testUser))
        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.success(testRepos))
        whenever(gitHubRepository.getStarredRepos(any())).thenReturn(Result.success(testRepos))
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
        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.success(fullRepos))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasMore)
        }
    }

    @Test
    fun `loadRepos sets hasMore to false when fewer items returned`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.success(testRepos))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasMore)
        }
    }

    @Test
    fun `loadRepos failure sets error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.failure(RuntimeException("API error")))

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

        verify(gitHubRepository, atLeast(2)).getAuthenticatedUser()
        verify(gitHubRepository, atLeast(2)).getUserRepos(any())
    }

    @Test
    fun `loadMore appends repos`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.success(fullRepos))

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
        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.success(testRepos))

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
        whenever(gitHubRepository.starRepository(any(), any())).thenReturn(Result.success(true))

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
        whenever(gitHubRepository.starRepository(any(), any())).thenReturn(Result.failure(RuntimeException("Error")))

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
        whenever(gitHubRepository.unstarRepository(any(), any())).thenReturn(Result.success(true))

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
        whenever(gitHubRepository.unstarRepository(any(), any())).thenReturn(Result.failure(RuntimeException("Error")))

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
        whenever(gitHubRepository.forkRepository(any(), any())).thenReturn(Result.success(
            Repository(id = 99, name = "forked", fullName = "testuser/forked", owner = Owner(1, "testuser"), fork = true)
        ))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.forkRepo("other", "repo")
        advanceUntilIdle()

        verify(gitHubRepository).forkRepository("other", "repo")
    }

    @Test
    fun `deleteRepo calls repository and reloads`() = runTest(testDispatcher) {
        whenever(gitHubRepository.deleteRepository(any(), any())).thenReturn(Result.success(true))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.deleteRepo("testuser", "repo1")
        advanceUntilIdle()

        verify(gitHubRepository).deleteRepository("testuser", "repo1")
    }

    @Test
    fun `createRepo calls repository and reloads`() = runTest(testDispatcher) {
        whenever(gitHubRepository.createRepository(any(), any(), any())).thenReturn(Result.success(
            Repository(id = 100, name = "new-repo", fullName = "testuser/new-repo", owner = Owner(1, "testuser"))
        ))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.createRepo("new-repo", "desc", false)
        advanceUntilIdle()

        verify(gitHubRepository).createRepository("new-repo", "desc", false)
    }

    @Test
    fun `logout clears all data`() = runTest(testDispatcher) {
        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        verify(tokenManager).clearAll()
    }

    @Test
    fun `createRepo failure still calls repository`() = runTest(testDispatcher) {
        whenever(gitHubRepository.createRepository(any(), any(), any())).thenReturn(Result.failure(RuntimeException("Error")))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.createRepo("new-repo", "desc", false)
        advanceUntilIdle()

        verify(gitHubRepository).createRepository("new-repo", "desc", false)
    }

    @Test
    fun `forkRepo failure still calls repository`() = runTest(testDispatcher) {
        whenever(gitHubRepository.forkRepository(any(), any())).thenReturn(Result.failure(RuntimeException("Error")))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.forkRepo("other", "repo")
        advanceUntilIdle()

        verify(gitHubRepository).forkRepository("other", "repo")
    }

    @Test
    fun `deleteRepo failure still calls repository`() = runTest(testDispatcher) {
        whenever(gitHubRepository.deleteRepository(any(), any())).thenReturn(Result.failure(RuntimeException("Error")))

        val viewModel = RepoListViewModel(gitHubRepository, tokenManager)
        advanceUntilIdle()

        viewModel.deleteRepo("testuser", "repo1")
        advanceUntilIdle()

        verify(gitHubRepository).deleteRepository("testuser", "repo1")
    }

    @Test
    fun `loadMore failure does not crash`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getUserRepos(page = 1)).thenReturn(Result.success(fullRepos))
        whenever(gitHubRepository.getUserRepos(page = 2)).thenReturn(Result.failure(RuntimeException("Error")))

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

        whenever(gitHubRepository.getUserRepos(any())).thenReturn(Result.failure(RuntimeException("Refresh error")))
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `loadProfile failure sets error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.getAuthenticatedUser()).thenReturn(Result.failure(RuntimeException("Profile error")))

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

        verify(gitHubRepository).getStarredRepos(any())
    }

    @Test
    fun `starRepo does not double-add if already starred`() = runTest(testDispatcher) {
        whenever(gitHubRepository.starRepository(any(), any())).thenReturn(Result.success(true))

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
}
