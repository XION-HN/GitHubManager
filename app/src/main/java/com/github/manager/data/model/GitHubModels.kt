package com.github.manager.data.model

import com.squareup.moshi.Json

data class User(
    val id: Long,
    val login: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "html_url") val htmlUrl: String = "",
    val bio: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "public_gists") val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

data class Repository(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val owner: Owner,
    val description: String? = null,
    val private: Boolean = false,
    val fork: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String = "",
    val language: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    @Json(name = "watchers_count") val watchersCount: Int = 0,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "pushed_at") val pushedAt: String? = null,
    val topics: List<String> = emptyList(),
    val archived: Boolean = false,
    val disabled: Boolean = false,
    val visibility: String? = null
)

data class Owner(
    val id: Long,
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "html_url") val htmlUrl: String = ""
)

data class Commit(
    val sha: String,
    val commit: CommitDetail,
    val author: User? = null,
    @Json(name = "html_url") val htmlUrl: String = ""
)

data class CommitDetail(
    val author: CommitAuthor,
    val message: String
)

data class CommitAuthor(
    val name: String,
    val email: String,
    val date: String
)

data class Issue(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String? = null,
    val state: String,
    val user: User? = null,
    val labels: List<Label> = emptyList(),
    val assignees: List<User> = emptyList(),
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "closed_at") val closedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String = "",
    @Json(name = "pull_request") val pullRequest: PullRequestRef? = null
)

data class Label(
    val id: Long,
    val name: String,
    val color: String,
    val description: String? = null
)

data class PullRequestRef(
    val url: String = "",
    @Json(name = "html_url") val htmlUrl: String = ""
)

data class PullRequest(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String? = null,
    val state: String,
    val user: User? = null,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "closed_at") val closedAt: String? = null,
    @Json(name = "merged_at") val mergedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String = "",
    val head: PRBranch,
    val base: PRBranch,
    val draft: Boolean = false,
    val mergeable: Boolean? = null
)

data class PRBranch(
    val ref: String,
    val sha: String,
    val label: String? = null
)

data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

data class CreateIssueRequest(
    val title: String,
    val body: String? = null,
    val labels: List<String>? = null
)

data class Branch(
    val name: String,
    val commit: BranchCommit,
    val protected: Boolean = false
)

data class BranchCommit(
    val sha: String,
    val url: String = ""
)

data class Workflow(
    val id: Long,
    val name: String,
    val path: String = "",
    val state: String = "active",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList()
)

data class WorkflowRun(
    val id: Long,
    val name: String = "",
    @Json(name = "head_branch") val headBranch: String = "",
    @Json(name = "head_sha") val headSha: String = "",
    val status: String = "",
    val conclusion: String? = null,
    @Json(name = "workflow_id") val workflowId: Long = 0,
    val url: String = "",
    @Json(name = "html_url") val htmlUrl: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "run_started_at") val runStartedAt: String? = null
)

data class CheckStarredResponse(
    val starred: Boolean = false
)

data class CachedData<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
)
