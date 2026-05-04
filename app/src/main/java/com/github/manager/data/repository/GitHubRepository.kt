package com.github.manager.data.repository

import com.github.manager.data.api.GitHubApiService
import com.github.manager.data.model.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val apiService: GitHubApiService
) {

    suspend fun getAuthenticatedUser(): Result<User> = safeApiCall {
        apiService.getAuthenticatedUser()
    }

    suspend fun getUserRepos(page: Int = 1): Result<List<Repository>> = safeApiCall {
        apiService.getUserRepos(page = page)
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
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun starRepository(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.starRepository(owner, repo)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unstarRepository(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.unstarRepository(owner, repo)
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
        apiService.getStarredRepos(page = page)
    }

    suspend fun forkRepository(owner: String, repo: String): Result<Repository> = safeApiCall {
        apiService.forkRepository(owner, repo)
    }

    suspend fun getCommits(owner: String, repo: String, branch: String? = null, page: Int = 1): Result<List<Commit>> = safeApiCall {
        apiService.getCommits(owner, repo, sha = branch, page = page)
    }

    suspend fun getCommitDetail(owner: String, repo: String, sha: String): Result<Commit> = safeApiCall {
        apiService.getCommitDetail(owner, repo, sha)
    }

    suspend fun getIssues(owner: String, repo: String, state: String = "open", page: Int = 1): Result<List<Issue>> = safeApiCall {
        apiService.getIssues(owner, repo, state, page = page)
    }

    suspend fun createIssue(owner: String, repo: String, title: String, body: String?): Result<Issue> = safeApiCall {
        apiService.createIssue(owner, repo, CreateIssueRequest(title, body))
    }

    suspend fun getPullRequests(owner: String, repo: String, state: String = "open", page: Int = 1): Result<List<PullRequest>> = safeApiCall {
        apiService.getPullRequests(owner, repo, state, page = page)
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

    suspend fun searchUsers(query: String, page: Int = 1): Result<UserSearchResult> = safeApiCall {
        val result = apiService.searchUsers(query, page = page)
        result.copy(items = result.items ?: emptyList())
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
        apiService.getReleases(owner, repo) ?: emptyList()
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
