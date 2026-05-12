package com.github.manager.data.local.db

import androidx.room.*

@Dao
interface RepoDao {
    @Query("SELECT * FROM cached_repos WHERE isStarred = 0 ORDER BY updatedAt DESC")
    suspend fun getMyRepos(): List<RepoEntity>

    @Query("SELECT * FROM cached_repos WHERE isStarred = 1 ORDER BY updatedAt DESC")
    suspend fun getStarredRepos(): List<RepoEntity>

    @Query("SELECT * FROM cached_repos WHERE fullName = :fullName LIMIT 1")
    suspend fun getRepoByFullName(fullName: String): RepoEntity?

    @Query("SELECT * FROM cached_repos WHERE name LIKE '%' || :query || '%' OR fullName LIKE '%' || :query || '%'")
    suspend fun searchRepos(query: String): List<RepoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<RepoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repo: RepoEntity)

    @Delete
    suspend fun delete(repo: RepoEntity)

    @Query("DELETE FROM cached_repos WHERE fullName = :fullName")
    suspend fun deleteByFullName(fullName: String)

    @Query("DELETE FROM cached_repos")
    suspend fun deleteAll()

    @Query("UPDATE cached_repos SET isStarred = :starred WHERE fullName = :fullName")
    suspend fun updateStarred(fullName: String, starred: Boolean)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM cached_users WHERE login = :login LIMIT 1")
    suspend fun getUserByLogin(login: String): UserEntity?

    @Query("SELECT * FROM cached_users WHERE login LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("DELETE FROM cached_users")
    suspend fun deleteAll()
}

@Dao
interface CommitDao {
    @Query("SELECT * FROM cached_commits WHERE repoFullName = :repoFullName ORDER BY authorDate DESC")
    suspend fun getCommits(repoFullName: String): List<CommitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commits: List<CommitEntity>)

    @Query("DELETE FROM cached_commits WHERE repoFullName = :repoFullName")
    suspend fun deleteByRepo(repoFullName: String)

    @Query("DELETE FROM cached_commits")
    suspend fun deleteAll()
}

@Dao
interface IssueDao {
    @Query("SELECT * FROM cached_issues WHERE repoFullName = :repoFullName AND isPullRequest = 0 AND state = :state ORDER BY updatedAt DESC")
    suspend fun getIssues(repoFullName: String, state: String): List<IssueEntity>

    @Query("SELECT * FROM cached_issues WHERE repoFullName = :repoFullName AND isPullRequest = 0 ORDER BY updatedAt DESC")
    suspend fun getIssuesAll(repoFullName: String): List<IssueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(issues: List<IssueEntity>)

    @Query("DELETE FROM cached_issues WHERE repoFullName = :repoFullName")
    suspend fun deleteByRepo(repoFullName: String)

    @Query("DELETE FROM cached_issues")
    suspend fun deleteAll()
}

@Dao
interface PullRequestDao {
    @Query("SELECT * FROM cached_pull_requests WHERE repoFullName = :repoFullName AND state = :state ORDER BY updatedAt DESC")
    suspend fun getPullRequests(repoFullName: String, state: String): List<PullRequestEntity>

    @Query("SELECT * FROM cached_pull_requests WHERE repoFullName = :repoFullName ORDER BY updatedAt DESC")
    suspend fun getPullRequestsAll(repoFullName: String): List<PullRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pullRequests: List<PullRequestEntity>)

    @Query("DELETE FROM cached_pull_requests WHERE repoFullName = :repoFullName")
    suspend fun deleteByRepo(repoFullName: String)

    @Query("DELETE FROM cached_pull_requests")
    suspend fun deleteAll()
}

@Dao
interface ReleaseDao {
    @Query("SELECT * FROM cached_releases WHERE repoFullName = :repoFullName ORDER BY publishedAt DESC")
    suspend fun getReleases(repoFullName: String): List<ReleaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(releases: List<ReleaseEntity>)

    @Query("DELETE FROM cached_releases WHERE repoFullName = :repoFullName")
    suspend fun deleteByRepo(repoFullName: String)

    @Query("DELETE FROM cached_releases")
    suspend fun deleteAll()
}
