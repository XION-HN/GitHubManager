package com.github.manager.data.repository

import com.github.manager.data.api.CreateIssueRequest
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

    suspend fun getStarredRepos(page: Int = 1): Result<List<Repository>> = safeApiCall {
        apiService.getStarredRepos(page = page)
    }

    suspend fun forkRepository(owner: String, repo: String): Result<Repository> = safeApiCall {
        apiService.forkRepository(owner, repo)
    }

    suspend fun getCommits(owner: String, repo: String, page: Int = 1): Result<List<Commit>> = safeApiCall {
        apiService.getCommits(owner, repo, page = page)
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

    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
