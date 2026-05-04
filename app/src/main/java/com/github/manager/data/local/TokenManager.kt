package com.github.manager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "github_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("github_token")
        private val USERNAME_KEY = stringPreferencesKey("github_username")
        private val CACHE_USER_KEY = stringPreferencesKey("cache_user")
        private val CACHE_REPOS_KEY = stringPreferencesKey("cache_repos")
        private val CACHE_STARRED_KEY = stringPreferencesKey("cache_starred")
        private val CACHE_PROFILE_KEY = stringPreferencesKey("cache_profile")
        private val LANGUAGE_MODE_KEY = stringPreferencesKey("language_mode")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[USERNAME_KEY] = username }
    }

    suspend fun saveLanguageMode(mode: String) {
        context.dataStore.edit { it[LANGUAGE_MODE_KEY] = mode }
    }

    suspend fun loadLanguageMode(): String? {
        return try {
            context.dataStore.data.first()[LANGUAGE_MODE_KEY]
        } catch (e: Exception) { null }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    suspend fun loadThemeMode(): String? {
        return try {
            context.dataStore.data.first()[THEME_MODE_KEY]
        } catch (e: Exception) { null }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USERNAME_KEY)
            prefs.remove(CACHE_USER_KEY)
            prefs.remove(CACHE_REPOS_KEY)
            prefs.remove(CACHE_STARRED_KEY)
            prefs.remove(CACHE_PROFILE_KEY)
        }
    }

    suspend fun saveCache(key: String, value: String) {
        val cacheKey = stringPreferencesKey("cache_$key")
        context.dataStore.edit { prefs -> prefs[cacheKey] = value }
    }

    suspend fun loadCache(key: String): String? {
        val cacheKey = stringPreferencesKey("cache_$key")
        return try {
            val prefs = context.dataStore.data.first()
            prefs[cacheKey]
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearCache(key: String) {
        val cacheKey = stringPreferencesKey("cache_$key")
        context.dataStore.edit { prefs -> prefs.remove(cacheKey) }
    }

    suspend fun clearAllCache() {
        context.dataStore.edit { prefs ->
            val keysToRemove = listOf(CACHE_USER_KEY, CACHE_REPOS_KEY, CACHE_STARRED_KEY, CACHE_PROFILE_KEY)
            keysToRemove.forEach { key -> prefs.remove(key) }
        }
    }
}
