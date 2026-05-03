package com.github.manager.data.api

import com.github.manager.data.model.*
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(): User

    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Repository>

    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Repository>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Repository

    @POST("user/repos")
    suspend fun createRepository(@Body request: CreateRepoRequest): Repository

    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @GET("user/starred")
    suspend fun getStarredRepos(
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Repository>

    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Repository

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Commit>

    @GET("repos/{owner}/{repo}/commits/{sha}")
    suspend fun getCommitDetail(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): Commit

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Issue>

    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun getIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): Issue

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body issue: CreateIssueRequest
    ): Issue

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<PullRequest>

    @GET("repos/{owner}/{repo}/pulls/{number}")
    suspend fun getPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): PullRequest
}

@JsonClass(generateAdapter = true)
data class CreateIssueRequest(
    val title: String,
    val body: String? = null,
    val labels: List<String>? = null
)
