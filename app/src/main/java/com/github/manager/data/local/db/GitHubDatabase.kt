package com.github.manager.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RepoEntity::class, UserEntity::class,
        CommitEntity::class, IssueEntity::class,
        PullRequestEntity::class, ReleaseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GitHubDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao
    abstract fun userDao(): UserDao
    abstract fun commitDao(): CommitDao
    abstract fun issueDao(): IssueDao
    abstract fun pullRequestDao(): PullRequestDao
    abstract fun releaseDao(): ReleaseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cached_commits` (
                        `dbId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `repoFullName` TEXT NOT NULL,
                        `sha` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL DEFAULT '',
                        `authorEmail` TEXT NOT NULL DEFAULT '',
                        `authorDate` TEXT NOT NULL DEFAULT '',
                        `message` TEXT NOT NULL DEFAULT '',
                        `authorLogin` TEXT,
                        `authorAvatarUrl` TEXT,
                        `htmlUrl` TEXT NOT NULL DEFAULT '',
                        `cachedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_commits_repoFullName_sha` ON `cached_commits` (`repoFullName`, `sha`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cached_issues` (
                        `dbId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `repoFullName` TEXT NOT NULL,
                        `id` INTEGER NOT NULL,
                        `number` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT,
                        `state` TEXT NOT NULL DEFAULT 'open',
                        `userLogin` TEXT,
                        `userAvatarUrl` TEXT,
                        `labels` TEXT,
                        `createdAt` TEXT NOT NULL DEFAULT '',
                        `updatedAt` TEXT NOT NULL DEFAULT '',
                        `closedAt` TEXT,
                        `htmlUrl` TEXT NOT NULL DEFAULT '',
                        `isPullRequest` INTEGER NOT NULL DEFAULT 0,
                        `cachedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_issues_repoFullName_number` ON `cached_issues` (`repoFullName`, `number`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cached_pull_requests` (
                        `dbId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `repoFullName` TEXT NOT NULL,
                        `id` INTEGER NOT NULL,
                        `number` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT,
                        `state` TEXT NOT NULL DEFAULT 'open',
                        `userLogin` TEXT,
                        `userAvatarUrl` TEXT,
                        `createdAt` TEXT NOT NULL DEFAULT '',
                        `updatedAt` TEXT NOT NULL DEFAULT '',
                        `closedAt` TEXT,
                        `mergedAt` TEXT,
                        `htmlUrl` TEXT NOT NULL DEFAULT '',
                        `headRef` TEXT NOT NULL DEFAULT '',
                        `headSha` TEXT NOT NULL DEFAULT '',
                        `baseRef` TEXT NOT NULL DEFAULT '',
                        `draft` INTEGER NOT NULL DEFAULT 0,
                        `cachedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_pull_requests_repoFullName_number` ON `cached_pull_requests` (`repoFullName`, `number`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cached_releases` (
                        `dbId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `repoFullName` TEXT NOT NULL,
                        `id` INTEGER NOT NULL,
                        `tagName` TEXT NOT NULL,
                        `name` TEXT,
                        `body` TEXT,
                        `draft` INTEGER NOT NULL DEFAULT 0,
                        `prerelease` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` TEXT NOT NULL DEFAULT '',
                        `publishedAt` TEXT,
                        `htmlUrl` TEXT NOT NULL DEFAULT '',
                        `authorLogin` TEXT,
                        `cachedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_releases_repoFullName_tagName` ON `cached_releases` (`repoFullName`, `tagName`)")
            }
        }
    }
}
