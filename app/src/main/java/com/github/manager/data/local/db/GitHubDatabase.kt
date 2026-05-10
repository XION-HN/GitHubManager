package com.github.manager.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RepoEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GitHubDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
    abstract fun userDao(): UserDao
}
