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
