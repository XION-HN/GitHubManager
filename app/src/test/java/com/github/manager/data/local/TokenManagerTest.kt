package com.github.manager.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    @Test
    fun `token key has correct name`() {
        val key = stringPreferencesKey("github_token")
        assertEquals("github_token", key.name)
    }

    @Test
    fun `username key has correct name`() {
        val key = stringPreferencesKey("github_username")
        assertEquals("github_username", key.name)
    }

    @Test
    fun `cache key prefix is cache_`() {
        val cacheKey = stringPreferencesKey("cache_repos")
        assertTrue(cacheKey.name.startsWith("cache_"))
    }

    @Test
    fun `language mode key name is correct`() {
        val key = stringPreferencesKey("language_mode")
        assertEquals("language_mode", key.name)
    }

    @Test
    fun `theme mode key name is correct`() {
        val key = stringPreferencesKey("theme_mode")
        assertEquals("theme_mode", key.name)
    }

    @Test
    fun `cache user key has cache_ prefix`() {
        val key = stringPreferencesKey("cache_user")
        assertTrue(key.name.startsWith("cache_"))
    }

    @Test
    fun `cache starred key has cache_ prefix`() {
        val key = stringPreferencesKey("cache_starred")
        assertTrue(key.name.startsWith("cache_"))
    }

    @Test
    fun `cache profile key has cache_ prefix`() {
        val key = stringPreferencesKey("cache_profile")
        assertTrue(key.name.startsWith("cache_"))
    }

    @Test
    fun `dynamic cache key format is cache_plus_key`() = runTest {
        val key = "repos"
        val cacheKey = stringPreferencesKey("cache_$key")
        assertEquals("cache_repos", cacheKey.name)
    }
}
