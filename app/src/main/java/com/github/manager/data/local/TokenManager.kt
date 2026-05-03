package com.github.manager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

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
        }
    }
}
