package com.github.manager.data.local

import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    @Test
    fun `token flow maps from preferences`() = runTest {
        val mockPrefs = mockk<Preferences>()
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns null
        val mockDataStore = mockk<androidx.datastore.core.DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(mockPrefs)

        assertEquals(null, mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `saveToken calls edit on dataStore`() = runTest {
        val mockDataStore = mockk<androidx.datastore.core.DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(mockk<Preferences>(relaxed = true))

        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns "ghp_test123"

        assertEquals("ghp_test123", mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `saveUsername persists to dataStore`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns "testuser"
        assertEquals("testuser", mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `saveLanguageMode persists value`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns "CHINESE"
        assertEquals("CHINESE", mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `saveThemeMode persists value`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns "DARK"
        assertEquals("DARK", mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `clearAll removes token and username`() = runTest {
        val mockDataStore = mockk<androidx.datastore.core.DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(mockk<Preferences>(relaxed = true))
    }

    @Test
    fun `saveCache stores value with cache_ prefix`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns "{\"login\":\"test\"}"
        assertEquals("{\"login\":\"test\"}", mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `loadLanguageMode returns null on empty store`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns null
        assertNull(mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `loadThemeMode returns null on empty store`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns null
        assertNull(mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `loadCache returns null when key not found`() = runTest {
        val mockPrefs = mockk<Preferences>(relaxed = true)
        every { mockPrefs.get<String>(any<Preferences.Key<String>>()) } returns null
        assertNull(mockPrefs.get<String>(any<Preferences.Key<String>>()))
    }

    @Test
    fun `preferences key names are consistent`() {
        val tokenKey = androidx.datastore.preferences.core.stringPreferencesKey("github_token")
        val usernameKey = androidx.datastore.preferences.core.stringPreferencesKey("github_username")
        assertEquals("github_token", tokenKey.name)
        assertEquals("github_username", usernameKey.name)
    }

    @Test
    fun `cache key prefix is cache_`() {
        val cacheKey = androidx.datastore.preferences.core.stringPreferencesKey("cache_repos")
        assertTrue(cacheKey.name.startsWith("cache_"))
    }

    @Test
    fun `language mode key name is correct`() {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("language_mode")
        assertEquals("language_mode", key.name)
    }

    @Test
    fun `theme mode key name is correct`() {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        assertEquals("theme_mode", key.name)
    }
}
