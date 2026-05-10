package com.github.manager.ui.screens.search

import app.cash.turbine.test
import com.github.manager.data.local.db.RepoDao
import com.github.manager.data.local.db.UserDao
import com.github.manager.data.model.Owner
import com.github.manager.data.model.Repository
import com.github.manager.data.model.SearchResult
import com.github.manager.data.model.User
import com.github.manager.data.model.UserSearchResult
import com.github.manager.data.repository.GitHubRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchViewModelTest {

    @Mock
    private lateinit var gitHubRepository: GitHubRepository

    @Mock
    private lateinit var repoDao: RepoDao

    @Mock
    private lateinit var userDao: UserDao

    private val testDispatcher = StandardTestDispatcher()

    private val testSearchResult = SearchResult(
        totalCount = 10,
        items = (1..5).map { i ->
            Repository(id = i.toLong(), name = "repo$i", fullName = "user/repo$i", owner = Owner(1, "user"))
        }
    )

    private val testUserSearchResult = UserSearchResult(
        totalCount = 5,
        items = (1..3).map { i ->
            User(id = i.toLong(), login = "user$i")
        }
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(gitHubRepository.searchRepositories(any(), any())).thenReturn(Result.success(testSearchResult))
        whenever(gitHubRepository.searchUsers(any(), any())).thenReturn(Result.success(testUserSearchResult))
        whenever(gitHubRepository.searchReposInCache(any())).thenReturn(emptyList())
        whenever(gitHubRepository.searchUsersInCache(any())).thenReturn(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty query and no results`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.query)
            assertTrue(state.repos.isEmpty())
            assertTrue(state.users.isEmpty())
            assertFalse(state.isLoading)
            assertTrue(state.isRepoSearch)
            assertTrue(state.showHistory)
        }
    }

    @Test
    fun `onQueryChanged with blank query clears results and shows history`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.query)
            assertTrue(state.repos.isEmpty())
            assertFalse(state.isLoading)
            assertTrue(state.showHistory)
        }
    }

    @Test
    fun `onQueryChanged triggers search after debounce`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        verify(gitHubRepository).searchRepositories(any(), page = 1)
    }

    @Test
    fun `onQueryChanged hides history when typing`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("k")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showHistory)
        }
    }

    @Test
    fun `onQueryChanged cancels previous search job`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kot")
        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        verify(gitHubRepository, atMost(1)).searchRepositories(any(), any())
    }

    @Test
    fun `search repositories populates repos list`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(5, state.repos.size)
            assertEquals(10, state.totalCount)
        }
    }

    @Test
    fun `search failure falls back to cache`() = runTest(testDispatcher) {
        whenever(gitHubRepository.searchRepositories(any(), any())).thenReturn(Result.failure(RuntimeException("API error")))
        whenever(gitHubRepository.searchReposInCache("kotlin")).thenReturn(
            listOf(Repository(id = 1, name = "cached-repo", fullName = "user/cached-repo", owner = Owner(1, "user")))
        )

        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOfflineFallback)
            assertEquals(1, state.repos.size)
            assertEquals("user/cached-repo", state.repos.first().fullName)
        }
    }

    @Test
    fun `search failure with empty cache sets error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.searchRepositories(any(), any())).thenReturn(Result.failure(RuntimeException("API error")))

        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `toggleSearchType to user search clears repos`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.toggleSearchType(false)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRepoSearch)
            assertTrue(state.repos.isEmpty())
        }
    }

    @Test
    fun `toggleSearchType to repo search clears users`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.toggleSearchType(false)
        viewModel.onQueryChanged("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.toggleSearchType(true)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isRepoSearch)
            assertTrue(state.users.isEmpty())
        }
    }

    @Test
    fun `user search populates users list`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.toggleSearchType(false)
        viewModel.onQueryChanged("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.users.size)
            assertEquals(5, state.totalCount)
        }
    }

    @Test
    fun `user search failure falls back to cache`() = runTest(testDispatcher) {
        whenever(gitHubRepository.searchUsers(any(), any())).thenReturn(Result.failure(RuntimeException("API error")))
        whenever(gitHubRepository.searchUsersInCache("test")).thenReturn(
            listOf(User(id = 1, login = "cached-user"))
        )

        val viewModel = SearchViewModel(gitHubRepository)
        viewModel.toggleSearchType(false)

        viewModel.onQueryChanged("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOfflineFallback)
            assertEquals(1, state.users.size)
        }
    }

    @Test
    fun `loadMore increments page and appends results`() = runTest(testDispatcher) {
        val page1Result = SearchResult(totalCount = 10, items = (1..5).map { i ->
            Repository(id = i.toLong(), name = "repo$i", fullName = "user/repo$i", owner = Owner(1, "user"))
        })
        val page2Result = SearchResult(totalCount = 10, items = (6..10).map { i ->
            Repository(id = i.toLong(), name = "repo$i", fullName = "user/repo$i", owner = Owner(1, "user"))
        })

        whenever(gitHubRepository.searchRepositories("kotlin", page = 1)).thenReturn(Result.success(page1Result))
        whenever(gitHubRepository.searchRepositories("kotlin", page = 2)).thenReturn(Result.success(page2Result))

        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(10, state.repos.size)
        }
    }

    @Test
    fun `loadMore does nothing when loading`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.loadMore()

        verify(gitHubRepository, never()).searchRepositories(any(), any())
    }

    @Test
    fun `loadMore does nothing when no more results`() = runTest(testDispatcher) {
        val smallResult = SearchResult(totalCount = 2, items = listOf(
            Repository(id = 1, name = "repo1", fullName = "user/repo1", owner = Owner(1, "user")),
            Repository(id = 2, name = "repo2", fullName = "user/repo2", owner = Owner(1, "user"))
        ))
        whenever(gitHubRepository.searchRepositories("rare", page = 1)).thenReturn(Result.success(smallResult))

        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("rare")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.loadMore()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasMore)
        }
    }

    @Test
    fun `hasMore is true when repos size less than totalCount`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasMore)
            assertTrue(state.repos.size < state.totalCount)
        }
    }

    @Test
    fun `toggleSearchType does not search when query is blank`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.toggleSearchType(false)

        verify(gitHubRepository, never()).searchUsers(any(), any())
    }

    @Test
    fun `setSortBy updates sort field`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.setSortBy("forks")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("forks", state.sortBy)
        }
    }

    @Test
    fun `setLanguageFilter updates language filter`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.setLanguageFilter("Kotlin")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Kotlin", state.languageFilter)
        }
    }

    @Test
    fun `setLanguageFilter to null clears filter`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.setLanguageFilter("Kotlin")
        viewModel.setLanguageFilter(null)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.languageFilter)
        }
    }

    @Test
    fun `addToHistory adds query to history`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.addToHistory("kotlin")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.searchHistory.contains("kotlin"))
        }
    }

    @Test
    fun `addToHistory does not add blank query`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.addToHistory("")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.searchHistory.contains(""))
        }
    }

    @Test
    fun `addToHistory moves duplicate to top`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.addToHistory("kotlin")
        viewModel.addToHistory("java")
        viewModel.addToHistory("kotlin")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("kotlin", state.searchHistory.first())
            assertEquals(2, state.searchHistory.size)
        }
    }

    @Test
    fun `removeFromHistory removes specific entry`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.addToHistory("kotlin")
        viewModel.addToHistory("java")
        viewModel.removeFromHistory("kotlin")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.searchHistory.contains("kotlin"))
            assertTrue(state.searchHistory.contains("java"))
        }
    }

    @Test
    fun `clearSearchHistory removes all entries`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.addToHistory("kotlin")
        viewModel.addToHistory("java")
        viewModel.clearSearchHistory()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.searchHistory.isEmpty())
        }
    }

    @Test
    fun `selectHistoryItem triggers search for that query`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.selectHistoryItem("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        verify(gitHubRepository).searchRepositories(any(), any())
    }

    @Test
    fun `loadMore failure sets error`() = runTest(testDispatcher) {
        val page1Result = SearchResult(totalCount = 10, items = (1..5).map { i ->
            Repository(id = i.toLong(), name = "repo$i", fullName = "user/repo$i", owner = Owner(1, "user"))
        })
        whenever(gitHubRepository.searchRepositories("kotlin", page = 1)).thenReturn(Result.success(page1Result))
        whenever(gitHubRepository.searchRepositories("kotlin", page = 2)).thenReturn(Result.failure(RuntimeException("Network error")))

        val viewModel = SearchViewModel(gitHubRepository)
        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `user search failure with empty cache sets error`() = runTest(testDispatcher) {
        whenever(gitHubRepository.searchUsers(any(), any())).thenReturn(Result.failure(RuntimeException("Error")))
        whenever(gitHubRepository.searchUsersInCache(any())).thenReturn(emptyList())

        val viewModel = SearchViewModel(gitHubRepository)
        viewModel.toggleSearchType(false)

        viewModel.onQueryChanged("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertTrue(state.users.isEmpty())
        }
    }

    @Test
    fun `setSortBy triggers new search`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.setSortBy("forks")
        advanceUntilIdle()

        verify(gitHubRepository, atLeast(2)).searchRepositories(any(), any())
    }

    @Test
    fun `setLanguageFilter triggers new search`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.setLanguageFilter("Kotlin")
        advanceUntilIdle()

        verify(gitHubRepository, atLeast(2)).searchRepositories(any(), any())
    }

    @Test
    fun `loadMore for user search appends results`() = runTest(testDispatcher) {
        val page1Result = UserSearchResult(totalCount = 10, items = (1..5).map { i ->
            User(id = i.toLong(), login = "user$i")
        })
        val page2Result = UserSearchResult(totalCount = 10, items = (6..10).map { i ->
            User(id = i.toLong(), login = "user$i")
        })

        whenever(gitHubRepository.searchUsers("test", page = 1)).thenReturn(Result.success(page1Result))
        whenever(gitHubRepository.searchUsers("test", page = 2)).thenReturn(Result.success(page2Result))

        val viewModel = SearchViewModel(gitHubRepository)
        viewModel.toggleSearchType(false)

        viewModel.onQueryChanged("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(10, state.users.size)
        }
    }

    @Test
    fun `initial state has totalCount zero`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.totalCount)
        }
    }

    @Test
    fun `onQueryChanged with same query after clear does not duplicate`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(gitHubRepository)

        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.onQueryChanged("")
        viewModel.onQueryChanged("kotlin")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(5, state.repos.size)
        }
    }
}
