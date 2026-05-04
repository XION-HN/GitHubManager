package com.github.manager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.manager.data.model.CachedData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
        private const val CACHE_TTL = 5 * 60 * 1000L
    }

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[USERNAME_KEY] = username }
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

    suspend fun <T> saveCache(key: String, data: T, serializer: (T) -> String) {
        val cacheKey = stringPreferencesKey("cache_$key")
        context.dataStore.edit { prefs ->
            prefs[cacheKey] = serializer(data)
        }
    }

    suspend fun <T> loadCache(key: String, deserializer: (String) -> T?): T? {
        val cacheKey = stringPreferencesKey("cache_$key")
        val json = context.dataStore.data.map { it[cacheKey] }
        return try {
            val value = kotlinx.coroutines.flow.firstOrNull(json) ?: return null
            deserializer(value)
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
            prefs.keys().filter { it.name.startsWith("cache_") }.forEach { prefs.remove(it) }
        }
    }

    private suspend fun <T> firstOrNull(flow: Flow<T?>): T? {
        return try {
            kotlinx.coroutines.flow.first(flow)
        } catch (e: Exception) {
            null
        }
    }
}
