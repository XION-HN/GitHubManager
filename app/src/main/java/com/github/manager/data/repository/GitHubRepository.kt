package com.github.manager.data.repository

import com.github.manager.data.api.GitHubApiService
import com.github.manager.data.local.db.*
import com.github.manager.data.model.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val apiService: GitHubApiService,
    private val repoDao: RepoDao,
    private val userDao: UserDao,
    private val commitDao: CommitDao,
    private val issueDao: IssueDao,
    private val pullRequestDao: PullRequestDao,
    private val releaseDao: ReleaseDao
) {

    suspend fun getAuthenticatedUser(): Result<User> = safeApiCall {
        val user = apiService.getAuthenticatedUser()
        userDao.insert(user.toEntity())
        user
    }

    suspend fun getAuthenticatedUserFromCache(): User? {
        return try {
            val entities = userDao.searchUsers("")
            entities.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserRepos(page: Int = 1): Result<List<Repository>> = safeApiCall {
        val repos = apiService.getUserRepos(page = page)
        if (page == 1) {
            val existingStarred = repoDao.getStarredRepos().map { it.fullName }.toSet()
            repoDao.insertAll(repos.map { it.toEntity(isStarred = existingStarred.contains(it.fullName)) })
        }
        repos
    }

    suspend fun getUserRepos(username: String, page: Int = 1): Result<List<Repository>> = safeApiCall {
        apiService.getUserRepos(username, page = page)
    }

    suspend fun getUserReposFromCache(): List<Repository> {
        return repoDao.getMyRepos().map { it.toDomain() }
    }

    suspend fun getRepository(owner: String, repo: String): Result<Repository> = safeApiCall {
        apiService.getRepository(owner, repo)
    }

    suspend fun createRepository(name: String, description: String?, isPrivate: Boolean): Result<Repository> = safeApiCall {
        apiService.createRepository(CreateRepoRequest(name, description, isPrivate))
    }

    suspend fun deleteRepository(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.deleteRepository(owner, repo)
            if (response.isSuccessful) {
                repoDao.deleteByFullName("$owner/$repo")
            }
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun starRepository(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.starRepository(owner, repo)
            if (response.isSuccessful) {
                repoDao.updateStarred("$owner/$repo", true)
            }
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unstarRepository(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.unstarRepository(owner, repo)
            if (response.isSuccessful) {
                repoDao.updateStarred("$owner/$repo", false)
            }
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkStarred(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.checkStarred(owner, repo)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStarredRepos(page: Int = 1): Result<List<Repository>> = safeApiCall {
        val repos = apiService.getStarredRepos(page = page)
        if (page == 1) {
            repoDao.insertAll(repos.map { it.toEntity(isStarred = true) })
        }
        repos
    }

    suspend fun getStarredReposFromCache(): List<Repository> {
        return repoDao.getStarredRepos().map { it.toDomain() }
    }

    suspend fun forkRepository(owner: String, repo: String): Result<Repository> = safeApiCall {
        apiService.forkRepository(owner, repo)
    }

    suspend fun getCommits(owner: String, repo: String, branch: String? = null, page: Int = 1): Result<List<Commit>> = safeApiCall {
        val commits = apiService.getCommits(owner, repo, sha = branch, page = page)
        if (page == 1) {
            val fullName = "$owner/$repo"
            commitDao.deleteByRepo(fullName)
            commitDao.insertAll(commits.map { it.toEntity(fullName) })
        }
        commits
    }

    suspend fun getCommitsFromCache(owner: String, repo: String): List<Commit> {
        return commitDao.getCommits("$owner/$repo").map { it.toDomain() }
    }

    suspend fun getCommitDetail(owner: String, repo: String, sha: String): Result<Commit> = safeApiCall {
        apiService.getCommitDetail(owner, repo, sha)
    }

    suspend fun getIssues(owner: String, repo: String, state: String = "open", page: Int = 1): Result<List<Issue>> = safeApiCall {
        val issues = apiService.getIssues(owner, repo, state, page = page)
        if (page == 1) {
            val fullName = "$owner/$repo"
            issueDao.deleteByRepo(fullName)
            issueDao.insertAll(issues.map { it.toEntity(fullName) })
        }
        issues
    }

    suspend fun getIssuesFromCache(owner: String, repo: String, state: String = "open"): List<Issue> {
        return try {
            if (state == "all") {
                issueDao.getIssuesAll("$owner/$repo").map { it.toDomain() }
            } else {
                issueDao.getIssues("$owner/$repo", state).map { it.toDomain() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createIssue(owner: String, repo: String, title: String, body: String?): Result<Issue> = safeApiCall {
        apiService.createIssue(owner, repo, CreateIssueRequest(title, body))
    }

    suspend fun getPullRequests(owner: String, repo: String, state: String = "open", page: Int = 1): Result<List<PullRequest>> = safeApiCall {
        val prs = apiService.getPullRequests(owner, repo, state, page = page)
        if (page == 1) {
            val fullName = "$owner/$repo"
            pullRequestDao.deleteByRepo(fullName)
            pullRequestDao.insertAll(prs.map { it.toEntity(fullName) })
        }
        prs
    }

    suspend fun getPullRequestsFromCache(owner: String, repo: String, state: String = "open"): List<PullRequest> {
        return try {
            if (state == "all") {
                pullRequestDao.getPullRequestsAll("$owner/$repo").map { it.toDomain() }
            } else {
                pullRequestDao.getPullRequests("$owner/$repo", state).map { it.toDomain() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPullRequest(owner: String, repo: String, number: Int): Result<PullRequest> = safeApiCall {
        apiService.getPullRequest(owner, repo, number)
    }

    suspend fun getBranches(owner: String, repo: String): Result<List<Branch>> = safeApiCall {
        apiService.getBranches(owner, repo)
    }

    suspend fun getWorkflows(owner: String, repo: String): Result<List<Workflow>> = safeApiCall {
        apiService.getWorkflows(owner, repo).workflows ?: emptyList()
    }

    suspend fun getWorkflowRuns(owner: String, repo: String, page: Int = 1): Result<WorkflowRunsResponse> = safeApiCall {
        val response = apiService.getWorkflowRuns(owner, repo, page = page)
        response.copy(workflowRuns = response.workflowRuns ?: emptyList())
    }

    suspend fun getWorkflowRun(owner: String, repo: String, runId: Long): Result<WorkflowRun> = safeApiCall {
        apiService.getWorkflowRun(owner, repo, runId).workflowRun ?: throw Exception("Run not found")
    }

    suspend fun dispatchWorkflow(owner: String, repo: String, workflowId: Long, ref: String): Result<Boolean> {
        return try {
            val response = apiService.dispatchWorkflow(owner, repo, workflowId, WorkflowDispatchRequest(ref))
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reRunWorkflow(owner: String, repo: String, runId: Long): Result<Boolean> {
        return try {
            val response = apiService.reRunWorkflow(owner, repo, runId)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelWorkflowRun(owner: String, repo: String, runId: Long): Result<Boolean> {
        return try {
            val response = apiService.cancelWorkflowRun(owner, repo, runId)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRepositories(query: String, page: Int = 1): Result<SearchResult> = safeApiCall {
        val result = apiService.searchRepositories(query, page = page)
        result.copy(items = result.items ?: emptyList())
    }

    suspend fun searchReposInCache(query: String): List<Repository> {
        return repoDao.searchRepos(query).map { it.toDomain() }
    }

    suspend fun searchUsers(query: String, page: Int = 1): Result<UserSearchResult> = safeApiCall {
        val result = apiService.searchUsers(query, page = page)
        result.copy(items = result.items ?: emptyList())
    }

    suspend fun searchUsersInCache(query: String): List<User> {
        return userDao.searchUsers(query).map { it.toDomain() }
    }

    suspend fun getRepoContent(owner: String, repo: String, path: String, ref: String? = null): Result<List<RepoContent>> = safeApiCall {
        apiService.getRepoContent(owner, repo, path, ref) ?: emptyList()
    }

    suspend fun getReadme(owner: String, repo: String, ref: String? = null): Result<RepoContent> = safeApiCall {
        apiService.getReadme(owner, repo, ref)
    }

    suspend fun getIssueComments(owner: String, repo: String, number: Int): Result<List<IssueComment>> = safeApiCall {
        apiService.getIssueComments(owner, repo, number) ?: emptyList()
    }

    suspend fun createIssueComment(owner: String, repo: String, number: Int, body: String): Result<IssueComment> = safeApiCall {
        apiService.createIssueComment(owner, repo, number, mapOf("body" to body))
    }

    suspend fun updateIssue(owner: String, repo: String, number: Int, state: String? = null, title: String? = null, body: String? = null): Result<Issue> = safeApiCall {
        apiService.updateIssue(owner, repo, number, UpdateIssueRequest(title, body, state))
    }

    suspend fun mergePullRequest(owner: String, repo: String, number: Int): Result<Boolean> {
        return try {
            val response = apiService.mergePullRequest(owner, repo, number, MergePRRequest())
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReleases(owner: String, repo: String): Result<List<Release>> = safeApiCall {
        val releases = apiService.getReleases(owner, repo) ?: emptyList()
        val fullName = "$owner/$repo"
        releaseDao.deleteByRepo(fullName)
        releaseDao.insertAll(releases.map { it.toEntity(fullName) })
        releases
    }

    suspend fun getReleasesFromCache(owner: String, repo: String): List<Release> {
        return releaseDao.getReleases("$owner/$repo").map { it.toDomain() }
    }

    suspend fun getFileContent(owner: String, repo: String, path: String, ref: String? = null): Result<RepoContent> = safeApiCall {
        apiService.getFileContent(owner, repo, path, ref)
    }

    suspend fun getNotifications(page: Int = 1): Result<List<Notification>> = safeApiCall {
        apiService.getNotifications(page = page) ?: emptyList()
    }

    suspend fun markNotificationRead(id: Long): Result<Boolean> {
        return try {
            val response = apiService.markNotificationRead(id)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(): Result<Boolean> {
        return try {
            val response = apiService.markAllNotificationsRead()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(username: String): Result<User> = safeApiCall {
        apiService.getUserProfile(username)
    }

    suspend fun getWorkflowRunJobs(owner: String, repo: String, runId: Long): Result<List<WorkflowJob>> = safeApiCall {
        apiService.getWorkflowRunJobs(owner, repo, runId).jobs ?: emptyList()
    }

    suspend fun getJobLogs(owner: String, repo: String, jobId: Long): Result<String> = safeApiCall {
        apiService.getJobLogs(owner, repo, jobId).string()
    }

    suspend fun clearAllCache() {
        repoDao.deleteAll()
        userDao.deleteAll()
        commitDao.deleteAll()
        issueDao.deleteAll()
        pullRequestDao.deleteAll()
        releaseDao.deleteAll()
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun Repository.toEntity(isStarred: Boolean = false): RepoEntity {
    return RepoEntity(
        id = id, name = name, fullName = fullName,
        ownerLogin = owner.login, ownerAvatarUrl = owner.avatarUrl,
        description = description, isPrivate = private, fork = fork,
        htmlUrl = htmlUrl, language = language, stargazersCount = stargazersCount,
        forksCount = forksCount, openIssuesCount = openIssuesCount,
        defaultBranch = defaultBranch, updatedAt = updatedAt,
        topics = topics?.joinToString(","), isStarred = isStarred
    )
}

private fun RepoEntity.toDomain(): Repository {
    return Repository(
        id = id, name = name, fullName = fullName,
        owner = Owner(id = 0, login = ownerLogin, avatarUrl = ownerAvatarUrl),
        description = description, private = isPrivate, fork = fork,
        htmlUrl = htmlUrl, language = language, stargazersCount = stargazersCount,
        forksCount = forksCount, openIssuesCount = openIssuesCount,
        defaultBranch = defaultBranch, updatedAt = updatedAt,
        topics = topics?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    )
}

private fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id, login = login, name = name,
        avatarUrl = avatarUrl, htmlUrl = htmlUrl, bio = bio,
        publicRepos = publicRepos, publicGists = publicGists,
        followers = followers, following = following, createdAt = createdAt
    )
}

private fun UserEntity.toDomain(): User {
    return User(
        id = id, login = login, name = name,
        avatarUrl = avatarUrl, htmlUrl = htmlUrl, bio = bio,
        publicRepos = publicRepos, publicGists = publicGists,
        followers = followers, following = following, createdAt = createdAt
    )
}

private fun Commit.toEntity(repoFullName: String): CommitEntity {
    return CommitEntity(
        repoFullName = repoFullName, sha = sha,
        authorName = commit.author.name, authorEmail = commit.author.email,
        authorDate = commit.author.date, message = commit.message,
        authorLogin = author?.login, authorAvatarUrl = author?.avatarUrl,
        htmlUrl = htmlUrl
    )
}

private fun CommitEntity.toDomain(): Commit {
    return Commit(
        sha = sha, htmlUrl = htmlUrl,
        commit = CommitDetail(
            author = CommitAuthor(name = authorName, email = authorEmail, date = authorDate),
            message = message
        ),
        author = authorLogin?.let {
            User(id = 0, login = it, avatarUrl = authorAvatarUrl ?: "")
        }
    )
}

private fun Issue.toEntity(repoFullName: String): IssueEntity {
    return IssueEntity(
        repoFullName = repoFullName, id = id, number = number,
        title = title, body = body, state = state,
        userLogin = user?.login, userAvatarUrl = user?.avatarUrl,
        labels = labels?.joinToString(",") { it.name },
        createdAt = createdAt, updatedAt = updatedAt,
        closedAt = closedAt, htmlUrl = htmlUrl,
        isPullRequest = pullRequest != null
    )
}

private fun IssueEntity.toDomain(): Issue {
    return Issue(
        id = id, number = number, title = title, body = body,
        state = state, htmlUrl = htmlUrl,
        user = userLogin?.let { User(id = 0, login = it, avatarUrl = userAvatarUrl ?: "") },
        labels = labels?.split(",")?.filter { it.isNotBlank() }?.map { Label(id = 0, name = it.trim(), color = "") },
        createdAt = createdAt, updatedAt = updatedAt, closedAt = closedAt,
        pullRequest = if (isPullRequest) PullRequestRef() else null
    )
}

private fun PullRequest.toEntity(repoFullName: String): PullRequestEntity {
    return PullRequestEntity(
        repoFullName = repoFullName, id = id, number = number,
        title = title, body = body, state = state,
        userLogin = user?.login, userAvatarUrl = user?.avatarUrl,
        createdAt = createdAt, updatedAt = updatedAt,
        closedAt = closedAt, mergedAt = mergedAt,
        htmlUrl = htmlUrl, headRef = head.ref, headSha = head.sha,
        baseRef = base.ref, draft = draft
    )
}

private fun PullRequestEntity.toDomain(): PullRequest {
    return PullRequest(
        id = id, number = number, title = title, body = body,
        state = state, htmlUrl = htmlUrl,
        user = userLogin?.let { User(id = 0, login = it, avatarUrl = userAvatarUrl ?: "") },
        createdAt = createdAt, updatedAt = updatedAt,
        closedAt = closedAt, mergedAt = mergedAt,
        head = PRBranch(ref = headRef, sha = headSha),
        base = PRBranch(ref = baseRef), draft = draft
    )
}

private fun Release.toEntity(repoFullName: String): ReleaseEntity {
    return ReleaseEntity(
        repoFullName = repoFullName, id = id, tagName = tagName,
        name = name, body = body, draft = draft, prerelease = prerelease,
        createdAt = createdAt, publishedAt = publishedAt,
        htmlUrl = htmlUrl, authorLogin = author?.login
    )
}

private fun ReleaseEntity.toDomain(): Release {
    return Release(
        id = id, tagName = tagName, name = name, body = body,
        draft = draft, prerelease = prerelease,
        createdAt = createdAt, publishedAt = publishedAt,
        htmlUrl = htmlUrl,
        author = authorLogin?.let { User(id = 0, login = it) }
    )
}
