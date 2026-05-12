package com.github.manager.ui.screens.profile

import com.github.manager.data.model.Owner
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
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
class UserProfileViewModelTest {

    private val gitHubRepository: GitHubRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = 1, login = "testuser", name = "Test User",
        bio = "A developer", publicRepos = 10, followers = 50, following = 10,
        createdAt = "2020-01-01T00:00:00Z"
    )

    private val testRepos = listOf(
        Repository(id = 1, name = "repo1", fullName = "testuser/repo1", owner = Owner(id = 1, login = "testuser")),
        Repository(id = 2, name = "repo2", fullName = "testuser/repo2", owner = Owner(id = 1, login = "testuser"))
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
    fun `loadUser success loads user and repos`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserProfile("testuser") } returns Result.success(testUser)
        coEvery { gitHubRepository.getUserRepos("testuser", page = 1) } returns Result.success(testRepos)

        val viewModel = UserProfileViewModel(gitHubRepository)
        viewModel.loadUser("testuser")
        advanceUntilIdle()

        assertEquals("testuser", viewModel.uiState.value.user?.login)
        assertEquals(2, viewModel.uiState.value.repos.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadUser failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserProfile("testuser") } returns Result.failure(RuntimeException("Not found"))

        val viewModel = UserProfileViewModel(gitHubRepository)
        viewModel.loadUser("testuser")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadUser does not reload same user`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getUserProfile("testuser") } returns Result.success(testUser)
        coEvery { gitHubRepository.getUserRepos("testuser", page = 1) } returns Result.success(testRepos)

        val viewModel = UserProfileViewModel(gitHubRepository)
        viewModel.loadUser("testuser")
        advanceUntilIdle()

        viewModel.loadUser("testuser")
        advanceUntilIdle()

        coVerify(exactly = 1) { gitHubRepository.getUserProfile("testuser") }
    }
}
