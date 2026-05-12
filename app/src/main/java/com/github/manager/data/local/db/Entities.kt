package com.github.manager.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cached_repos", indices = [Index(value = ["fullName"], unique = true)])
data class RepoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String?,
    @ColumnInfo(name = "is_private") val isPrivate: Boolean = false,
    val fork: Boolean = false,
    val htmlUrl: String = "",
    val language: String? = null,
    val stargazersCount: Int = 0,
    val forksCount: Int = 0,
    val openIssuesCount: Int = 0,
    val defaultBranch: String = "main",
    val updatedAt: String = "",
    val topics: String? = null,
    val isStarred: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_users", indices = [Index(value = ["login"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: Long,
    val login: String,
    val name: String?,
    val avatarUrl: String = "",
    val htmlUrl: String = "",
    val bio: String? = null,
    val publicRepos: Int = 0,
    val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val createdAt: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_commits",
    indices = [Index(value = ["repoFullName", "sha"], unique = true)]
)
data class CommitEntity(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    val repoFullName: String,
    val sha: String,
    val authorName: String = "",
    val authorEmail: String = "",
    val authorDate: String = "",
    val message: String = "",
    val authorLogin: String? = null,
    val authorAvatarUrl: String? = null,
    val htmlUrl: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_issues",
    indices = [Index(value = ["repoFullName", "number"], unique = true)]
)
data class IssueEntity(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    val repoFullName: String,
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String = "open",
    val userLogin: String? = null,
    val userAvatarUrl: String? = null,
    val labels: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val closedAt: String? = null,
    val htmlUrl: String = "",
    val isPullRequest: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_pull_requests",
    indices = [Index(value = ["repoFullName", "number"], unique = true)]
)
data class PullRequestEntity(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    val repoFullName: String,
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String = "open",
    val userLogin: String? = null,
    val userAvatarUrl: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val closedAt: String? = null,
    val mergedAt: String? = null,
    val htmlUrl: String = "",
    val headRef: String = "",
    val headSha: String = "",
    val baseRef: String = "",
    val draft: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_releases",
    indices = [Index(value = ["repoFullName", "tagName"], unique = true)]
)
data class ReleaseEntity(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    val repoFullName: String,
    val id: Long,
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val createdAt: String = "",
    val publishedAt: String? = null,
    val htmlUrl: String = "",
    val authorLogin: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
