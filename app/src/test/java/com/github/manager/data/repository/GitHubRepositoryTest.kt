package com.github.manager.data.repository

import com.github.manager.data.api.GitHubApiService
import com.github.manager.data.local.db.RepoDao
import com.github.manager.data.local.db.UserDao
import com.github.manager.data.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GitHubRepositoryTest {

    private val apiService: GitHubApiService = mockk()

    private val repoDao: RepoDao = mockk()

    private val userDao: UserDao = mockk()

    private val commitDao: CommitDao = mockk()

    private val issueDao: IssueDao = mockk()

    private val pullRequestDao: PullRequestDao = mockk()

    private val releaseDao: ReleaseDao = mockk()

    private lateinit var repository: GitHubRepository

    @Before
    fun setUp() {
        repository = GitHubRepository(apiService, repoDao, userDao, commitDao, issueDao, pullRequestDao, releaseDao)
    }

    @Test
    fun `getAuthenticatedUser success`() = runTest {
        val user = User(id = 1, login = "testuser", name = "Test User")
        coEvery { apiService.getAuthenticatedUser() } returns user
        coEvery { userDao.insert(any()) } just Runs

        val result = repository.getAuthenticatedUser()

        assertTrue(result.isSuccess)
        assertEquals("testuser", result.getOrNull()?.login)
        coVerify { userDao.insert(any()) }
    }

    @Test
    fun `getAuthenticatedUser failure`() = runTest {
        coEvery { apiService.getAuthenticatedUser() } throws RuntimeException("Network error")

        val result = repository.getAuthenticatedUser()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getUserRepos success caches repos`() = runTest {
        val repos = listOf(
            Repository(id = 1, name = "repo1", fullName = "user/repo1", owner = Owner(1, "user")),
            Repository(id = 2, name = "repo2", fullName = "user/repo2", owner = Owner(1, "user"))
        )
        coEvery { apiService.getUserRepos(page = 1) } returns repos
        coEvery { repoDao.getStarredRepos() } returns emptyList()
        coEvery { repoDao.insertAll(any()) } just Runs

        val result = repository.getUserRepos(page = 1)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify { repoDao.insertAll(any()) }
    }

    @Test
    fun `getUserRepos failure`() = runTest {
        coEvery { apiService.getUserRepos(page = 1) } throws RuntimeException("API error")

        val result = repository.getUserRepos(page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getUserReposFromCache returns cached repos`() = runTest {
    val entities = listOf(
        com.github.manager.data.local.db.RepoEntity(
            id = 1, name = "repo1", fullName = "user/repo1",
            ownerLogin = "user", ownerAvatarUrl = "", description = null
        )
    )
    coEvery { repoDao.getMyRepos() } returns entities

        val result = repository.getUserReposFromCache()

        assertEquals(1, result.size)
        assertEquals("user/repo1", result.first().fullName)
    }

    @Test
    fun `getRepository success`() = runTest {
        val repo = Repository(id = 1, name = "repo1", fullName = "owner/repo1", owner = Owner(1, "owner"))
        coEvery { apiService.getRepository("owner", "repo1") } returns repo

        val result = repository.getRepository("owner", "repo1")

        assertTrue(result.isSuccess)
        assertEquals("owner/repo1", result.getOrNull()?.fullName)
    }

    @Test
    fun `createRepository success`() = runTest {
        val created = Repository(id = 1, name = "new-repo", fullName = "user/new-repo", owner = Owner(1, "user"))
        coEvery { apiService.createRepository(any()) } returns created

        val result = repository.createRepository("new-repo", "desc", true)

        assertTrue(result.isSuccess)
        assertEquals("new-repo", result.getOrNull()?.name)
    }

    @Test
    fun `deleteRepository success removes from cache`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.deleteRepository("owner", "repo") } returns response
        coEvery { repoDao.deleteByFullName("owner/repo") } just Runs

        val result = repository.deleteRepository("owner", "repo")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
        coVerify { repoDao.deleteByFullName("owner/repo") }
    }

    @Test
    fun `deleteRepository api failure returns false`() = runTest {
        val response: Response<Unit> = Response.error(404, okhttp3.ResponseBody.create(null, "Not Found"))
        coEvery { apiService.deleteRepository("owner", "repo") } returns response

        val result = repository.deleteRepository("owner", "repo")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `deleteRepository exception failure`() = runTest {
        coEvery { apiService.deleteRepository("owner", "repo") } throws RuntimeException("Error")

        val result = repository.deleteRepository("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `starRepository success updates cache`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.starRepository("owner", "repo") } returns response
        coEvery { repoDao.updateStarred("owner/repo", true) } just Runs

        val result = repository.starRepository("owner", "repo")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
        coVerify { repoDao.updateStarred("owner/repo", true) }
    }

    @Test
    fun `unstarRepository success updates cache`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.unstarRepository("owner", "repo") } returns response
        coEvery { repoDao.updateStarred("owner/repo", false) } just Runs

        val result = repository.unstarRepository("owner", "repo")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
        coVerify { repoDao.updateStarred("owner/repo", false) }
    }

    @Test
    fun `checkStarred returns true when starred`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.checkStarred("owner", "repo") } returns response

        val result = repository.checkStarred("owner", "repo")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `checkStarred returns false when not starred`() = runTest {
        val response: Response<Unit> = Response.error(404, okhttp3.ResponseBody.create(null, "Not Found"))
        coEvery { apiService.checkStarred("owner", "repo") } returns response

        val result = repository.checkStarred("owner", "repo")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `getStarredRepos success caches repos`() = runTest {
        val repos = listOf(Repository(id = 1, name = "starred", fullName = "other/starred", owner = Owner(2, "other")))
        coEvery { apiService.getStarredRepos(page = 1) } returns repos
        coEvery { repoDao.insertAll(any()) } just Runs

        val result = repository.getStarredRepos(page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        coVerify { repoDao.insertAll(any()) }
    }

    @Test
    fun `getStarredReposFromCache returns cached starred repos`() = runTest {
    val entities = listOf(
        com.github.manager.data.local.db.RepoEntity(
            id = 1, name = "starred", fullName = "other/starred",
            ownerLogin = "other", ownerAvatarUrl = "", description = null, isStarred = true
        )
    )
    coEvery { repoDao.getStarredRepos() } returns entities

        val result = repository.getStarredReposFromCache()

        assertEquals(1, result.size)
        assertEquals("other/starred", result.first().fullName)
    }

    @Test
    fun `forkRepository success`() = runTest {
        val forked = Repository(id = 2, name = "forked-repo", fullName = "user/forked-repo", owner = Owner(1, "user"), fork = true)
        coEvery { apiService.forkRepository("owner", "repo") } returns forked

        val result = repository.forkRepository("owner", "repo")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.fork!!)
    }

    @Test
    fun `getCommits success caches commits`() = runTest {
        val commits = listOf(Commit(sha = "abc123", commit = CommitDetail(CommitAuthor("user", "e@e.com", "2024-01-01"), "fix: bug")))
        coEvery { apiService.getCommits("owner", "repo", sha = null, page = 1) } returns commits
        coEvery { commitDao.deleteByRepo("owner/repo") } just Runs
        coEvery { commitDao.insertAll(any()) } just Runs

        val result = repository.getCommits("owner", "repo", page = 1)

        assertTrue(result.isSuccess)
        assertEquals("abc123", result.getOrNull()?.first()?.sha)
        coVerify { commitDao.insertAll(any()) }
    }

    @Test
    fun `getCommits with branch`() = runTest {
        val commits = listOf(Commit(sha = "def456"))
        coEvery { apiService.getCommits("owner", "repo", sha = "develop", page = 1) } returns commits
        coEvery { commitDao.deleteByRepo("owner/repo") } just Runs
        coEvery { commitDao.insertAll(any()) } just Runs

        val result = repository.getCommits("owner", "repo", branch = "develop", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getIssues success caches issues`() = runTest {
        val issues = listOf(Issue(id = 1, number = 1, title = "Bug", state = "open"))
        coEvery { apiService.getIssues("owner", "repo", "open", page = 1) } returns issues
        coEvery { issueDao.deleteByRepo("owner/repo") } just Runs
        coEvery { issueDao.insertAll(any()) } just Runs

        val result = repository.getIssues("owner", "repo", state = "open", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        coVerify { issueDao.insertAll(any()) }
    }

    @Test
    fun `createIssue success`() = runTest {
        val issue = Issue(id = 1, number = 1, title = "New Issue")
        coEvery { apiService.createIssue(eq("owner"), eq("repo"), any()) } returns issue

        val result = repository.createIssue("owner", "repo", "New Issue", "body")

        assertTrue(result.isSuccess)
        assertEquals("New Issue", result.getOrNull()?.title)
    }

    @Test
    fun `getPullRequests success caches PRs`() = runTest {
        val prs = listOf(PullRequest(id = 1, number = 1, title = "PR 1", state = "open"))
        coEvery { apiService.getPullRequests("owner", "repo", "open", page = 1) } returns prs
        coEvery { pullRequestDao.deleteByRepo("owner/repo") } just Runs
        coEvery { pullRequestDao.insertAll(any()) } just Runs

        val result = repository.getPullRequests("owner", "repo", state = "open", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        coVerify { pullRequestDao.insertAll(any()) }
    }

    @Test
    fun `getBranches success`() = runTest {
        val branches = listOf(Branch(name = "main"), Branch(name = "develop"))
        coEvery { apiService.getBranches("owner", "repo") } returns branches

        val result = repository.getBranches("owner", "repo")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun `getWorkflows success`() = runTest {
        val workflows = listOf(Workflow(id = 1, name = "CI"))
        val response = WorkflowListResponse(totalCount = 1, workflows = workflows)
        coEvery { apiService.getWorkflows("owner", "repo") } returns response

        val result = repository.getWorkflows("owner", "repo")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getWorkflows null list returns empty`() = runTest {
        val response = WorkflowListResponse(totalCount = 0, workflows = null)
        coEvery { apiService.getWorkflows("owner", "repo") } returns response

        val result = repository.getWorkflows("owner", "repo")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getWorkflowRuns success`() = runTest {
        val runs = listOf(WorkflowRun(id = 1, name = "CI", status = "completed", conclusion = "success"))
        val response = WorkflowRunsResponse(totalCount = 1, workflowRuns = runs)
        coEvery { apiService.getWorkflowRuns("owner", "repo", page = 1) } returns response

        val result = repository.getWorkflowRuns("owner", "repo", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.workflowRuns?.size)
    }

    @Test
    fun `getWorkflowRun success`() = runTest {
        val run = WorkflowRun(id = 1, name = "CI", status = "completed")
        val wrapper = WorkflowRunResponseWrapper(workflowRun = run)
        coEvery { apiService.getWorkflowRun("owner", "repo", 1L) } returns wrapper

        val result = repository.getWorkflowRun("owner", "repo", 1L)

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull()?.id)
    }

    @Test
    fun `getWorkflowRun null throws exception`() = runTest {
        val wrapper = WorkflowRunResponseWrapper(workflowRun = null)
        coEvery { apiService.getWorkflowRun("owner", "repo", 1L) } returns wrapper

        val result = repository.getWorkflowRun("owner", "repo", 1L)

        assertTrue(result.isFailure)
    }

    @Test
    fun `dispatchWorkflow success`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.dispatchWorkflow(eq("owner"), eq("repo"), eq(1L), any()) } returns response

        val result = repository.dispatchWorkflow("owner", "repo", 1L, "main")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `reRunWorkflow success`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.reRunWorkflow("owner", "repo", 1L) } returns response

        val result = repository.reRunWorkflow("owner", "repo", 1L)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `cancelWorkflowRun success`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.cancelWorkflowRun("owner", "repo", 1L) } returns response

        val result = repository.cancelWorkflowRun("owner", "repo", 1L)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `searchRepositories success`() = runTest {
        val repos = listOf(Repository(id = 1, name = "kotlin", fullName = "jetbrains/kotlin", owner = Owner(1, "jetbrains")))
        val searchResult = SearchResult(totalCount = 1, items = repos)
        coEvery { apiService.searchRepositories("kotlin", page = 1) } returns searchResult

        val result = repository.searchRepositories("kotlin", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.totalCount)
        assertEquals(1, result.getOrNull()?.items?.size)
    }

    @Test
    fun `searchRepositories null items returns empty list`() = runTest {
        val searchResult = SearchResult(totalCount = 0, items = null)
        coEvery { apiService.searchRepositories("kotlin", page = 1) } returns searchResult

        val result = repository.searchRepositories("kotlin", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.items?.size)
    }

    @Test
    fun `searchReposInCache queries local database`() = runTest {
    val entities = listOf(
        com.github.manager.data.local.db.RepoEntity(
            id = 1, name = "kotlin", fullName = "jetbrains/kotlin",
            ownerLogin = "jetbrains", ownerAvatarUrl = "", description = null
        )
    )
    coEvery { repoDao.searchRepos("kotlin") } returns entities

        val result = repository.searchReposInCache("kotlin")

        assertEquals(1, result.size)
        assertEquals("jetbrains/kotlin", result.first().fullName)
    }

    @Test
    fun `searchUsers success`() = runTest {
        val users = listOf(User(id = 1, login = "testuser"))
        val searchResult = UserSearchResult(totalCount = 1, items = users)
        coEvery { apiService.searchUsers("test", page = 1) } returns searchResult

        val result = repository.searchUsers("test", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.totalCount)
    }

    @Test
    fun `searchUsersInCache queries local database`() = runTest {
        val entities = listOf(
            com.github.manager.data.local.db.UserEntity(
                id = 1, login = "testuser", name = "Test User"
            )
        )
        coEvery { userDao.searchUsers("test") } returns entities

        val result = repository.searchUsersInCache("test")

        assertEquals(1, result.size)
        assertEquals("testuser", result.first().login)
    }

    @Test
    fun `getRepoContent success`() = runTest {
        val contents = listOf(RepoContent(name = "src", type = "dir"), RepoContent(name = "README.md", type = "file"))
        coEvery { apiService.getRepoContent("owner", "repo", "", null) } returns contents

        val result = repository.getRepoContent("owner", "repo", "")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun `getReadme success`() = runTest {
        val readme = RepoContent(name = "README.md", content = "SGVsbG8=")
        coEvery { apiService.getReadme("owner", "repo", null) } returns readme

        val result = repository.getReadme("owner", "repo")

        assertTrue(result.isSuccess)
        assertEquals("README.md", result.getOrNull()?.name)
    }

    @Test
    fun `getIssueComments success`() = runTest {
        val comments = listOf(IssueComment(id = 1, body = "Nice fix"))
        coEvery { apiService.getIssueComments("owner", "repo", 1) } returns comments

        val result = repository.getIssueComments("owner", "repo", 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `createIssueComment success`() = runTest {
        val comment = IssueComment(id = 1, body = "New comment")
        coEvery { apiService.createIssueComment(eq("owner"), eq("repo"), eq(1), any()) } returns comment

        val result = repository.createIssueComment("owner", "repo", 1, "New comment")

        assertTrue(result.isSuccess)
        assertEquals("New comment", result.getOrNull()?.body)
    }

    @Test
    fun `updateIssue success`() = runTest {
        val updated = Issue(id = 1, number = 1, title = "Updated", state = "closed")
        coEvery { apiService.updateIssue(eq("owner"), eq("repo"), eq(1), any()) } returns updated

        val result = repository.updateIssue("owner", "repo", 1, state = "closed")

        assertTrue(result.isSuccess)
        assertEquals("closed", result.getOrNull()?.state)
    }

    @Test
    fun `mergePullRequest success`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.mergePullRequest(eq("owner"), eq("repo"), eq(1), any()) } returns response

        val result = repository.mergePullRequest("owner", "repo", 1)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `mergePullRequest failure`() = runTest {
        coEvery { apiService.mergePullRequest(eq("owner"), eq("repo"), eq(1), any()) } throws RuntimeException("Conflict")

        val result = repository.mergePullRequest("owner", "repo", 1)

        assertTrue(result.isFailure)
        assertEquals("Conflict", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getReleases success caches releases`() = runTest {
        val releases = listOf(Release(id = 1, tagName = "v1.0.0", name = "First Release"))
        coEvery { apiService.getReleases("owner", "repo") } returns releases
        coEvery { releaseDao.deleteByRepo("owner/repo") } just Runs
        coEvery { releaseDao.insertAll(any()) } just Runs

        val result = repository.getReleases("owner", "repo")

        assertTrue(result.isSuccess)
        assertEquals("v1.0.0", result.getOrNull()?.first()?.tagName)
        coVerify { releaseDao.insertAll(any()) }
    }

    @Test
    fun `clearAllCache clears all DAOs`() = runTest {
        coEvery { repoDao.deleteAll() } just Runs
        coEvery { userDao.deleteAll() } just Runs
        coEvery { commitDao.deleteAll() } just Runs
        coEvery { issueDao.deleteAll() } just Runs
        coEvery { pullRequestDao.deleteAll() } just Runs
        coEvery { releaseDao.deleteAll() } just Runs

        repository.clearAllCache()

        coVerify { repoDao.deleteAll() }
        coVerify { userDao.deleteAll() }
        coVerify { commitDao.deleteAll() }
        coVerify { issueDao.deleteAll() }
        coVerify { pullRequestDao.deleteAll() }
        coVerify { releaseDao.deleteAll() }
    }

    @Test
    fun `getCommitDetail success`() = runTest {
        val commit = Commit(sha = "abc123", commit = CommitDetail(CommitAuthor("user", "e@e.com", "2024-01-01"), "fix: bug"))
        coEvery { apiService.getCommitDetail("owner", "repo", "abc123") } returns commit

        val result = repository.getCommitDetail("owner", "repo", "abc123")

        assertTrue(result.isSuccess)
        assertEquals("abc123", result.getOrNull()?.sha)
    }

    @Test
    fun `getPullRequest success`() = runTest {
        val pr = PullRequest(id = 1, number = 42, title = "Feature PR")
        coEvery { apiService.getPullRequest("owner", "repo", 42) } returns pr

        val result = repository.getPullRequest("owner", "repo", 42)

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull()?.number)
    }

    @Test
    fun `getRepository failure`() = runTest {
        coEvery { apiService.getRepository("owner", "repo") } throws RuntimeException("Not Found")

        val result = repository.getRepository("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `createRepository failure`() = runTest {
        coEvery { apiService.createRepository(any()) } throws RuntimeException("Unprocessable Entity")

        val result = repository.createRepository("repo", null, false)

        assertTrue(result.isFailure)
    }

    @Test
    fun `forkRepository failure`() = runTest {
        coEvery { apiService.forkRepository("owner", "repo") } throws RuntimeException("Forbidden")

        val result = repository.forkRepository("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCommits failure`() = runTest {
        coEvery { apiService.getCommits("owner", "repo", sha = null, page = 1) } throws RuntimeException("Server Error")

        val result = repository.getCommits("owner", "repo", page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getIssues failure`() = runTest {
        coEvery { apiService.getIssues("owner", "repo", "open", page = 1) } throws RuntimeException("Error")

        val result = repository.getIssues("owner", "repo", state = "open", page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `createIssue failure`() = runTest {
        coEvery { apiService.createIssue(eq("owner"), eq("repo"), any()) } throws RuntimeException("Error")

        val result = repository.createIssue("owner", "repo", "Title", "Body")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getPullRequests failure`() = runTest {
        coEvery { apiService.getPullRequests("owner", "repo", "open", page = 1) } throws RuntimeException("Error")

        val result = repository.getPullRequests("owner", "repo", state = "open", page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getBranches failure`() = runTest {
        coEvery { apiService.getBranches("owner", "repo") } throws RuntimeException("Error")

        val result = repository.getBranches("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getWorkflows failure`() = runTest {
        coEvery { apiService.getWorkflows("owner", "repo") } throws RuntimeException("Error")

        val result = repository.getWorkflows("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getWorkflowRuns failure`() = runTest {
        coEvery { apiService.getWorkflowRuns("owner", "repo", page = 1) } throws RuntimeException("Error")

        val result = repository.getWorkflowRuns("owner", "repo", page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `dispatchWorkflow failure`() = runTest {
        coEvery { apiService.dispatchWorkflow(eq("owner"), eq("repo"), eq(1L), any()) } throws RuntimeException("Error")

        val result = repository.dispatchWorkflow("owner", "repo", 1L, "main")

        assertTrue(result.isFailure)
    }

    @Test
    fun `reRunWorkflow failure`() = runTest {
        coEvery { apiService.reRunWorkflow("owner", "repo", 1L) } throws RuntimeException("Error")

        val result = repository.reRunWorkflow("owner", "repo", 1L)

        assertTrue(result.isFailure)
    }

    @Test
    fun `cancelWorkflowRun failure`() = runTest {
        coEvery { apiService.cancelWorkflowRun("owner", "repo", 1L) } throws RuntimeException("Error")

        val result = repository.cancelWorkflowRun("owner", "repo", 1L)

        assertTrue(result.isFailure)
    }

    @Test
    fun `starRepository failure`() = runTest {
        coEvery { apiService.starRepository("owner", "repo") } throws RuntimeException("Error")

        val result = repository.starRepository("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `unstarRepository failure`() = runTest {
        coEvery { apiService.unstarRepository("owner", "repo") } throws RuntimeException("Error")

        val result = repository.unstarRepository("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `checkStarred exception`() = runTest {
        coEvery { apiService.checkStarred("owner", "repo") } throws RuntimeException("Network error")

        val result = repository.checkStarred("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `searchRepositories failure`() = runTest {
        coEvery { apiService.searchRepositories("kotlin", page = 1) } throws RuntimeException("Error")

        val result = repository.searchRepositories("kotlin", page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `searchUsers success with items`() = runTest {
        val users = listOf(User(id = 1, login = "octocat"), User(id = 2, login = "torvalds"))
        val searchResult = UserSearchResult(totalCount = 2, items = users)
        coEvery { apiService.searchUsers("test", page = 1) } returns searchResult

        val result = repository.searchUsers("test", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.items?.size)
        assertEquals("octocat", result.getOrNull()?.items?.first()?.login)
    }

    @Test
    fun `searchUsers with null items returns empty list`() = runTest {
        val searchResult = UserSearchResult(totalCount = 0, items = null)
        coEvery { apiService.searchUsers("test", page = 1) } returns searchResult

        val result = repository.searchUsers("test", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.items?.size)
    }

    @Test
    fun `searchUsers failure`() = runTest {
        coEvery { apiService.searchUsers("test", page = 1) } throws RuntimeException("Error")

        val result = repository.searchUsers("test", page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getRepoContent failure`() = runTest {
        coEvery { apiService.getRepoContent("owner", "repo", "", null) } throws RuntimeException("Error")

        val result = repository.getRepoContent("owner", "repo", "")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getReadme failure`() = runTest {
        coEvery { apiService.getReadme("owner", "repo", null) } throws RuntimeException("Error")

        val result = repository.getReadme("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getIssueComments failure`() = runTest {
        coEvery { apiService.getIssueComments("owner", "repo", 1) } throws RuntimeException("Error")

        val result = repository.getIssueComments("owner", "repo", 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `createIssueComment failure`() = runTest {
        coEvery { apiService.createIssueComment(eq("owner"), eq("repo"), eq(1), any()) } throws RuntimeException("Error")

        val result = repository.createIssueComment("owner", "repo", 1, "Comment")

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateIssue failure`() = runTest {
        coEvery { apiService.updateIssue(eq("owner"), eq("repo"), eq(1), any()) } throws RuntimeException("Error")

        val result = repository.updateIssue("owner", "repo", 1, state = "closed")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getReleases failure`() = runTest {
        coEvery { apiService.getReleases("owner", "repo") } throws RuntimeException("Error")

        val result = repository.getReleases("owner", "repo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCommitDetail failure`() = runTest {
        coEvery { apiService.getCommitDetail("owner", "repo", "abc123") } throws RuntimeException("Error")

        val result = repository.getCommitDetail("owner", "repo", "abc123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getPullRequest failure`() = runTest {
        coEvery { apiService.getPullRequest("owner", "repo", 42) } throws RuntimeException("Error")

        val result = repository.getPullRequest("owner", "repo", 42)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getWorkflowRuns with null runs returns empty list`() = runTest {
        val response = WorkflowRunsResponse(totalCount = 0, workflowRuns = null)
        coEvery { apiService.getWorkflowRuns("owner", "repo", page = 1) } returns response

        val result = repository.getWorkflowRuns("owner", "repo", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.workflowRuns?.size)
    }

    @Test
    fun `getUserRepos does not cache on page 2`() = runTest {
        val repos = listOf(Repository(id = 1, name = "repo1", fullName = "user/repo1", owner = Owner(1, "user")))
        coEvery { apiService.getUserRepos(page = 2) } returns repos

        val result = repository.getUserRepos(page = 2)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { repoDao.insertAll(any()) }
    }

    @Test
    fun `getUserRepos by username success`() = runTest {
        val repos = listOf(Repository(id = 1, name = "repo1", fullName = "other/repo1", owner = Owner(2, "other")))
        coEvery { apiService.getUserRepos("other", page = 1) } returns repos

        val result = repository.getUserRepos("other", page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getFileContent success`() = runTest {
        val content = RepoContent(name = "Main.kt", type = "file", content = "ZnVuYyBtYWluKCk=")
        coEvery { apiService.getFileContent("owner", "repo", "src/Main.kt", null) } returns content

        val result = repository.getFileContent("owner", "repo", "src/Main.kt")

        assertTrue(result.isSuccess)
        assertEquals("Main.kt", result.getOrNull()?.name)
    }

    @Test
    fun `getFileContent failure`() = runTest {
        coEvery { apiService.getFileContent("owner", "repo", "src/Main.kt", null) } throws RuntimeException("Not Found")

        val result = repository.getFileContent("owner", "repo", "src/Main.kt")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getNotifications success`() = runTest {
        val notifications = listOf(
            Notification(id = 1, unread = true, reason = "subscribe", subject = NotificationSubject(title = "Bug"), repository = NotificationRepo(id = 1, name = "repo", fullName = "owner/repo", owner = Owner(id = 1, login = "owner")))
        )
        coEvery { apiService.getNotifications(page = 1) } returns notifications

        val result = repository.getNotifications(page = 1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getNotifications failure`() = runTest {
        coEvery { apiService.getNotifications(page = 1) } throws RuntimeException("Error")

        val result = repository.getNotifications(page = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `markNotificationRead success`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.markNotificationRead(1L) } returns response

        val result = repository.markNotificationRead(1L)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `markNotificationRead failure`() = runTest {
        coEvery { apiService.markNotificationRead(1L) } throws RuntimeException("Error")

        val result = repository.markNotificationRead(1L)

        assertTrue(result.isFailure)
    }

    @Test
    fun `markAllNotificationsRead success`() = runTest {
        val response: Response<Unit> = Response.success(Unit)
        coEvery { apiService.markAllNotificationsRead() } returns response

        val result = repository.markAllNotificationsRead()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `getUserProfile success`() = runTest {
        val user = User(id = 1, login = "octocat", name = "The Octocat")
        coEvery { apiService.getUserProfile("octocat") } returns user

        val result = repository.getUserProfile("octocat")

        assertTrue(result.isSuccess)
        assertEquals("octocat", result.getOrNull()?.login)
    }

    @Test
    fun `getUserProfile failure`() = runTest {
        coEvery { apiService.getUserProfile("octocat") } throws RuntimeException("Not Found")

        val result = repository.getUserProfile("octocat")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getWorkflowRunJobs success`() = runTest {
        val jobs = listOf(WorkflowJob(id = 1, name = "build", status = "completed", conclusion = "success"))
        val response = WorkflowJobsResponse(totalCount = 1, jobs = jobs)
        coEvery { apiService.getWorkflowRunJobs("owner", "repo", 1L) } returns response

        val result = repository.getWorkflowRunJobs("owner", "repo", 1L)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getWorkflowRunJobs null jobs returns empty`() = runTest {
        val response = WorkflowJobsResponse(totalCount = 0, jobs = null)
        coEvery { apiService.getWorkflowRunJobs("owner", "repo", 1L) } returns response

        val result = repository.getWorkflowRunJobs("owner", "repo", 1L)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getWorkflowRunJobs failure`() = runTest {
        coEvery { apiService.getWorkflowRunJobs("owner", "repo", 1L) } throws RuntimeException("Error")

        val result = repository.getWorkflowRunJobs("owner", "repo", 1L)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCommitsFromCache returns cached commits`() = runTest {
        val entities = listOf(
            CommitEntity(repoFullName = "owner/repo", sha = "abc123",
                authorName = "user", authorEmail = "e@e.com", authorDate = "2024-01-01",
                message = "fix: bug", htmlUrl = "http://example.com")
        )
        coEvery { commitDao.getCommits("owner/repo") } returns entities

        val result = repository.getCommitsFromCache("owner", "repo")

        assertEquals(1, result.size)
        assertEquals("abc123", result.first().sha)
    }

    @Test
    fun `getIssuesFromCache returns cached issues`() = runTest {
        val entities = listOf(
            IssueEntity(repoFullName = "owner/repo", id = 1, number = 1,
                title = "Bug", state = "open", isPullRequest = false)
        )
        coEvery { issueDao.getIssues("owner/repo", "open") } returns entities

        val result = repository.getIssuesFromCache("owner", "repo", "open")

        assertEquals(1, result.size)
        assertEquals(1, result.first().number)
        assertNull(result.first().pullRequest)
    }

    @Test
    fun `getIssuesFromCache with all state queries all`() = runTest {
        val entities = listOf(
            IssueEntity(repoFullName = "owner/repo", id = 1, number = 1,
                title = "Bug", state = "closed", isPullRequest = false)
        )
        coEvery { issueDao.getIssuesAll("owner/repo") } returns entities

        val result = repository.getIssuesFromCache("owner", "repo", "all")

        assertEquals(1, result.size)
    }

    @Test
    fun `getPullRequestsFromCache returns cached PRs`() = runTest {
        val entities = listOf(
            PullRequestEntity(repoFullName = "owner/repo", id = 1, number = 1,
                title = "PR 1", state = "open", headRef = "feature", baseRef = "main")
        )
        coEvery { pullRequestDao.getPullRequests("owner/repo", "open") } returns entities

        val result = repository.getPullRequestsFromCache("owner", "repo", "open")

        assertEquals(1, result.size)
        assertEquals("feature", result.first().head.ref)
    }

    @Test
    fun `getReleasesFromCache returns cached releases`() = runTest {
        val entities = listOf(
            ReleaseEntity(repoFullName = "owner/repo", id = 1, tagName = "v1.0.0",
                name = "First Release")
        )
        coEvery { releaseDao.getReleases("owner/repo") } returns entities

        val result = repository.getReleasesFromCache("owner", "repo")

        assertEquals(1, result.size)
        assertEquals("v1.0.0", result.first().tagName)
    }

    @Test
    fun `getCommits does not cache on page 2`() = runTest {
        val commits = listOf(Commit(sha = "abc123"))
        coEvery { apiService.getCommits("owner", "repo", sha = null, page = 2) } returns commits

        val result = repository.getCommits("owner", "repo", page = 2)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { commitDao.insertAll(any()) }
    }

    @Test
    fun `getIssuesFromCache failure returns empty list`() = runTest {
        coEvery { issueDao.getIssues("owner/repo", "open") } throws RuntimeException("DB error")

        val result = repository.getIssuesFromCache("owner", "repo", "open")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPullRequestsFromCache failure returns empty list`() = runTest {
        coEvery { pullRequestDao.getPullRequests("owner/repo", "open") } throws RuntimeException("DB error")

        val result = repository.getPullRequestsFromCache("owner", "repo", "open")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `markAllNotificationsRead failure`() = runTest {
        coEvery { apiService.markAllNotificationsRead() } throws RuntimeException("Error")

        val result = repository.markAllNotificationsRead()

        assertTrue(result.isFailure)
    }
}
