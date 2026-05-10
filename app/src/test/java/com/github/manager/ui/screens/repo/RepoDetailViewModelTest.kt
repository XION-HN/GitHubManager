package com.github.manager.ui.screens.repo

import app.cash.turbine.test
import com.github.manager.data.model.*
import com.github.manager.data.repository.GitHubRepository
import io.mockk.*
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
class RepoDetailViewModelTest {

    private val gitHubRepository: GitHubRepository = mockk()

    private val testDispatcher = StandardTestDispatcher()

    private val testRepo = Repository(
        id = 1, name = "test-repo", fullName = "owner/test-repo",
        owner = Owner(1, "owner"), defaultBranch = "main"
    )

    private val testCommits = listOf(
        Commit(sha = "abc123", commit = CommitDetail(CommitAuthor("user", "e@e.com", "2024-01-01"), "fix: bug")),
        Commit(sha = "def456", commit = CommitDetail(CommitAuthor("user", "e@e.com", "2024-01-02"), "feat: add"))
    )

    private val testIssues = listOf(
        Issue(id = 1, number = 1, title = "Bug", state = "open"),
        Issue(id = 2, number = 2, title = "PR Issue", state = "open", pullRequest = PullRequestRef())
    )

    private val testPRs = listOf(
        PullRequest(id = 1, number = 1, title = "Feature PR", state = "open", mergeable = true)
    )

    private val testBranches = listOf(
        Branch(name = "main", commit = BranchCommit("abc123")),
        Branch(name = "develop", commit = BranchCommit("def456"))
    )

    private val testWorkflows = listOf(
        Workflow(id = 1, name = "CI", state = "active")
    )

    private val testWorkflowRuns = WorkflowRunsResponse(
        totalCount = 1,
        workflowRuns = listOf(WorkflowRun(id = 1, name = "CI", status = "completed", conclusion = "success"))
    )

    private val testReleases = listOf(
        Release(id = 1, tagName = "v1.0.0", name = "First Release")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { gitHubRepository.getRepository("owner", "test-repo") } returns Result.success(testRepo)
        coEvery { gitHubRepository.checkStarred("owner", "test-repo") } returns Result.success(true)
        coEvery { gitHubRepository.getCommits(any(), any(), any(), any()) } returns Result.success(testCommits)
        coEvery { gitHubRepository.getIssues(any(), any(), any(), any()) } returns Result.success(testIssues)
        coEvery { gitHubRepository.getPullRequests(any(), any(), any(), any()) } returns Result.success(testPRs)
        coEvery { gitHubRepository.getBranches(any(), any()) } returns Result.success(testBranches)
        coEvery { gitHubRepository.getWorkflows(any(), any()) } returns Result.success(testWorkflows)
        coEvery { gitHubRepository.getWorkflowRuns(any(), any(), any()) } returns Result.success(testWorkflowRuns)
        coEvery { gitHubRepository.getReleases(any(), any()) } returns Result.success(testReleases)
        coEvery { gitHubRepository.getRepoContent(any(), any(), any(), any()) } returns Result.success(emptyList())
        coEvery { gitHubRepository.getReadme(any(), any(), any()) } returns Result.success(RepoContent())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads repo, starred status, and commits`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.repo)
            assertEquals("owner/test-repo", state.repo?.fullName)
            assertTrue(state.isStarred)
            assertEquals(2, state.commits.size)
        }
    }

    @Test
    fun `init does not reinitialize if same owner and repo`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        coVerify(exactly = 1) { gitHubRepository.getRepository("owner", "test-repo") }
    }

    @Test
    fun `init reinitializes if different repo`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        coEvery { gitHubRepository.getRepository("owner", "other-repo") } returns Result.success(
            testRepo.copy(name = "other-repo", fullName = "owner/other-repo")
        )
        coEvery { gitHubRepository.checkStarred("owner", "other-repo") } returns Result.success(false)

        viewModel.init("owner", "other-repo")
        advanceUntilIdle()

        coVerify { gitHubRepository.getRepository("owner", "other-repo") }
    }

    @Test
    fun `toggleStar from starred to unstarred`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.unstarRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.toggleStar()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isStarred)
        }
    }

    @Test
    fun `toggleStar from unstarred to starred`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.checkStarred(any(), any()) } returns Result.success(false)
        coEvery { gitHubRepository.starRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.toggleStar()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStarred)
        }
    }

    @Test
    fun `onTabChanged loads issues for tab 1`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.issues.size)
            assertEquals(1, state.currentTab)
        }
    }

    @Test
    fun `onTabChanged filters out PRs from issues`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            state.issues.forEach { issue ->
                assertNull(issue.pullRequest)
            }
        }
    }

    @Test
    fun `onTabChanged loads PRs for tab 2`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(2)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.pullRequests.size)
            assertEquals(2, state.currentTab)
        }
    }

    @Test
    fun `onTabChanged loads branches for tab 3`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(3)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.branches.size)
        }
    }

    @Test
    fun `onTabChanged loads files for tab 4`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(4)
        advanceUntilIdle()

        coVerify { gitHubRepository.getRepoContent(eq("owner"), eq("test-repo"), eq(""), any()) }
    }

    @Test
    fun `onTabChanged loads workflows for tab 5`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(5)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.workflows.size)
        }
    }

    @Test
    fun `onTabChanged loads releases for tab 6`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(6)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.releases.size)
            assertEquals("v1.0.0", state.releases.first().tagName)
        }
    }

    @Test
    fun `switchBranch updates currentBranch and reloads commits`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.switchBranch("develop")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("develop", state.currentBranch)
        }

        coVerify(atLeast = 2) { gitHubRepository.getCommits(eq("owner"), eq("test-repo"), eq("develop"), any()) }
    }

    @Test
    fun `closeIssue updates issue state`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.updateIssue(any(), any(), any(), state = eq("closed")) } returns Result.success(
            Issue(id = 1, number = 1, state = "closed")
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.closeIssue(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Issue #1 closed", state.actionMessage)
        }
    }

    @Test
    fun `reopenIssue updates issue state`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.updateIssue(any(), any(), any(), state = eq("open")) } returns Result.success(
            Issue(id = 1, number = 1, state = "open")
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.reopenIssue(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Issue #1 reopened", state.actionMessage)
        }
    }

    @Test
    fun `mergePullRequest updates PR state`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.mergePullRequest(any(), any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.mergePullRequest(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("PR #1 merged", state.actionMessage)
        }
    }

    @Test
    fun `loadIssueComments loads comments`() = runTest(testDispatcher) {
        val comments = listOf(IssueComment(id = 1, body = "Nice fix"))
        coEvery { gitHubRepository.getIssueComments("owner", "test-repo", 1) } returns Result.success(comments)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadIssueComments(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.issueComments.size)
            assertEquals(1, state.selectedIssueNumber)
        }
    }

    @Test
    fun `addComment creates comment and reloads`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.createIssueComment(any(), any(), any(), any()) } returns
            Result.success(IssueComment(id = 2, body = "New comment"))
        coEvery { gitHubRepository.getIssueComments(any(), any(), any()) } returns Result.success(emptyList())

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadIssueComments(1)
        advanceUntilIdle()

        viewModel.addComment("New comment")
        advanceUntilIdle()

        coVerify { gitHubRepository.createIssueComment("owner", "test-repo", 1, "New comment") }
    }

    @Test
    fun `addComment does nothing when no issue selected`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.addComment("Comment")

        coVerify(exactly = 0) { gitHubRepository.createIssueComment(any(), any(), any(), any()) }
    }

    @Test
    fun `clearActionMessage sets actionMessage to null`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.clearActionMessage()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.actionMessage)
        }
    }

    @Test
    fun `loadFiles sorts directories first`() = runTest(testDispatcher) {
        val files = listOf(
            RepoContent(name = "README.md", type = "file"),
            RepoContent(name = "src", type = "dir"),
            RepoContent(name = "build.gradle", type = "file")
        )
        coEvery { gitHubRepository.getRepoContent(any(), any(), eq(""), any()) } returns Result.success(files)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadFiles("")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("src", state.files.first().name)
            assertEquals("dir", state.files.first().type)
        }
    }

    @Test
    fun `navigateUp goes to parent directory`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getRepoContent(any(), any(), eq("src"), any()) } returns Result.success(
            listOf(RepoContent(name = "main.kt", type = "file", path = "src/main.kt"))
        )
        coEvery { gitHubRepository.getRepoContent(any(), any(), eq(""), any()) } returns Result.success(
            listOf(RepoContent(name = "src", type = "dir", path = "src"))
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadFiles("src")
        advanceUntilIdle()

        viewModel.navigateUp()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.currentPath)
        }
    }

    @Test
    fun `dispatchWorkflow triggers workflow`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.dispatchWorkflow(any(), any(), any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.dispatchWorkflow(1L, "main")
        advanceUntilIdle()

        coVerify { gitHubRepository.dispatchWorkflow("owner", "test-repo", 1L, "main") }
    }

    @Test
    fun `reRunWorkflow calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.reRunWorkflow(any(), any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.reRunWorkflow(1L)
        advanceUntilIdle()

        coVerify { gitHubRepository.reRunWorkflow("owner", "test-repo", 1L) }
    }

    @Test
    fun `cancelWorkflowRun calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.cancelWorkflowRun(any(), any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.cancelWorkflowRun(1L)
        advanceUntilIdle()

        coVerify { gitHubRepository.cancelWorkflowRun("owner", "test-repo", 1L) }
    }

    @Test
    fun `forkRepo calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.forkRepository(any(), any()) } returns Result.success(
            Repository(id = 99, name = "test-repo", fullName = "user/test-repo", owner = Owner(1, "user"), fork = true)
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.forkRepo()
        advanceUntilIdle()

        coVerify { gitHubRepository.forkRepository("owner", "test-repo") }
    }

    @Test
    fun `deleteRepo calls repository`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.deleteRepository(any(), any()) } returns Result.success(true)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.deleteRepo()
        advanceUntilIdle()

        coVerify { gitHubRepository.deleteRepository("owner", "test-repo") }
    }

    @Test
    fun `createIssue calls repository and reloads`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.createIssue(any(), any(), any(), any()) } returns Result.success(
            Issue(id = 3, number = 3, title = "New Issue")
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.createIssue("New Issue", "Body")
        advanceUntilIdle()

        coVerify { gitHubRepository.createIssue("owner", "test-repo", "New Issue", "Body") }
    }

    @Test
    fun `loadCommits failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getCommits(any(), any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `checkStarred failure sets isStarred to false`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.checkStarred(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isStarred)
        }
    }

    @Test
    fun `toggleStar failure keeps current state`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.unstarRepository(any(), any()) } returns Result.failure(RuntimeException("Unstar failed"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.toggleStar()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStarred)
        }
    }

    @Test
    fun `closeIssue failure sets actionMessage`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.updateIssue(any(), any(), any(), state = eq("closed")) } returns Result.failure(
            RuntimeException("Update failed")
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.closeIssue(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `mergePullRequest failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.mergePullRequest(any(), any(), any()) } returns Result.failure(RuntimeException("Merge conflict"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.mergePullRequest(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `loadIssueComments failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getIssueComments("owner", "test-repo", 1) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadIssueComments(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `addComment failure does not crash`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getIssueComments("owner", "test-repo", 1) } returns Result.success(emptyList())
        coEvery { gitHubRepository.createIssueComment(any(), any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadIssueComments(1)
        advanceUntilIdle()

        viewModel.addComment("Test comment")
        advanceUntilIdle()

        coVerify { gitHubRepository.createIssueComment("owner", "test-repo", 1, "Test comment") }
    }

    @Test
    fun `switchBranch failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getCommits(any(), any(), eq("nonexistent"), any()) } returns Result.failure(RuntimeException("Branch not found"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.switchBranch("nonexistent")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("nonexistent", state.currentBranch)
            assertNotNull(state.error)
        }
    }

    @Test
    fun `loadFiles failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getRepoContent(any(), any(), eq("src"), any()) } returns Result.failure(RuntimeException("Not found"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadFiles("src")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `dispatchWorkflow failure sets actionMessage`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.dispatchWorkflow(any(), any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.dispatchWorkflow(1L, "main")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.actionMessage)
        }
    }

    @Test
    fun `forkRepo failure does not crash`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.forkRepository(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.forkRepo()
        advanceUntilIdle()

        coVerify { gitHubRepository.forkRepository("owner", "test-repo") }
    }

    @Test
    fun `deleteRepo failure does not crash`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.deleteRepository(any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.deleteRepo()
        advanceUntilIdle()

        coVerify { gitHubRepository.deleteRepository("owner", "test-repo") }
    }

    @Test
    fun `createIssue failure does not crash`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.createIssue(any(), any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.createIssue("New Issue", "Body")
        advanceUntilIdle()

        coVerify { gitHubRepository.createIssue("owner", "test-repo", "New Issue", "Body") }
    }

    @Test
    fun `loadRepos failure on init sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getRepository("owner", "test-repo") } returns Result.failure(RuntimeException("Not Found"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.repo)
            assertNotNull(state.error)
        }
    }

    @Test
    fun `onTabChanged loads issues with state filter`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getIssues(any(), any(), eq("closed"), any()) } returns Result.success(
            listOf(Issue(id = 3, number = 3, title = "Closed Bug", state = "closed"))
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.setIssueFilter("closed")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("closed", state.issueStateFilter)
        }
    }

    @Test
    fun `onTabChanged sets pr state filter`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.getPullRequests(any(), any(), eq("closed"), any()) } returns Result.success(
            listOf(PullRequest(id = 2, number = 2, title = "Merged PR", state = "closed"))
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.onTabChanged(2)
        advanceUntilIdle()

        viewModel.setPrFilter("closed")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("closed", state.prStateFilter)
        }
    }

    @Test
    fun `loadFiles navigates into subdirectory`() = runTest(testDispatcher) {
        val rootFiles = listOf(RepoContent(name = "src", type = "dir", path = "src"))
        val srcFiles = listOf(RepoContent(name = "Main.kt", type = "file", path = "src/Main.kt"))

        coEvery { gitHubRepository.getRepoContent(any(), any(), eq(""), any()) } returns Result.success(rootFiles)
        coEvery { gitHubRepository.getRepoContent(any(), any(), eq("src"), any()) } returns Result.success(srcFiles)

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.loadFiles("")
        advanceUntilIdle()

        viewModel.loadFiles("src")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("src", state.currentPath)
            assertEquals(1, state.files.size)
            assertEquals("Main.kt", state.files.first().name)
        }
    }

    @Test
    fun `refresh reloads current tab data`() = runTest(testDispatcher) {
        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(atLeast = 2) { gitHubRepository.getCommits(any(), any(), any(), any()) }
    }

    @Test
    fun `reopenIssue failure sets error`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.updateIssue(any(), any(), any(), state = eq("open")) } returns Result.failure(
            RuntimeException("Error")
        )

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.reopenIssue(1)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `cancelWorkflowRun failure sets actionMessage`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.cancelWorkflowRun(any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.cancelWorkflowRun(1L)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.actionMessage)
        }
    }

    @Test
    fun `reRunWorkflow failure sets actionMessage`() = runTest(testDispatcher) {
        coEvery { gitHubRepository.reRunWorkflow(any(), any(), any()) } returns Result.failure(RuntimeException("Error"))

        val viewModel = RepoDetailViewModel(gitHubRepository)
        viewModel.init("owner", "test-repo")
        advanceUntilIdle()

        viewModel.reRunWorkflow(1L)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.actionMessage)
        }
    }
}
