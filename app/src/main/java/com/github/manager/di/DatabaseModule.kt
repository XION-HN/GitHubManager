package com.github.manager.di

import android.content.Context
import androidx.room.Room
import com.github.manager.data.local.db.GitHubDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GitHubDatabase {
        return Room.databaseBuilder(
            context,
            GitHubDatabase::class.java,
            "github_manager_db"
        ).build()
    }

    @Provides
    fun provideRepoDao(database: GitHubDatabase) = database.repoDao()

    @Provides
    fun provideUserDao(database: GitHubDatabase) = database.userDao()
}
