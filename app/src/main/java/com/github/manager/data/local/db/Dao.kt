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
