package com.github.manager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDataStore: DataStore<Preferences>

    private lateinit var tokenManager: TokenManager

    private var preferencesMap = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        preferencesMap = mutableMapOf()

        val prefs = mutablePreferencesOf()
        preferencesMap.forEach { (key, value) ->
            prefs[stringPreferencesKey(key)] = value
        }

        whenever(mockDataStore.data).thenReturn(flowOf(prefs))

        whenever(mockDataStore.edit(any())).thenAnswer { invocation ->
            val transform = invocation.getArgument<(Preferences) -> Unit>(0)
            val currentPrefs = mutablePreferencesOf()
            preferencesMap.forEach { (key, value) ->
                currentPrefs[stringPreferencesKey(key)] = value
            }
            transform(currentPrefs)
            currentPrefs.asMap().forEach { (key, value) ->
                if (value != null) {
                    preferencesMap[key.name] = value.toString()
                } else {
                    preferencesMap.remove(key.name)
                }
            }
            Unit
        }

        tokenManager = TokenManager(mockContext)
    }

    @Test
    fun `saveToken and read token flow`() = runTest {
        tokenManager.saveToken("ghp_test123")
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `saveUsername persists to dataStore`() = runTest {
        tokenManager.saveUsername("testuser")
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `saveLanguageMode persists value`() = runTest {
        tokenManager.saveLanguageMode("CHINESE")
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `saveThemeMode persists value`() = runTest {
        tokenManager.saveThemeMode("DARK")
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `clearAll removes token and username`() = runTest {
        tokenManager.clearAll()
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `saveCache stores value with cache_ prefix`() = runTest {
        tokenManager.saveCache("user", "{\"login\":\"test\"}")
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `clearCache removes specific cache key`() = runTest {
        tokenManager.saveCache("repos", "[]")
        tokenManager.clearCache("repos")
        verify(mockDataStore, atLeast(2)).edit(any())
    }

    @Test
    fun `clearAllCache removes predefined cache keys`() = runTest {
        tokenManager.clearAllCache()
        verify(mockDataStore).edit(any())
    }

    @Test
    fun `loadLanguageMode returns null on empty store`() = runTest {
        val result = tokenManager.loadLanguageMode()
        assertNull(result)
    }

    @Test
    fun `loadThemeMode returns null on empty store`() = runTest {
        val result = tokenManager.loadThemeMode()
        assertNull(result)
    }

    @Test
    fun `loadCache returns null when key not found`() = runTest {
        val result = tokenManager.loadCache("nonexistent")
        assertNull(result)
    }

    @Test
    fun `saveToken then read via preferencesMap`() = runTest {
        tokenManager.saveToken("ghp_roundtrip")
        assertEquals("ghp_roundtrip", preferencesMap["github_token"])
    }

    @Test
    fun `saveUsername then read via preferencesMap`() = runTest {
        tokenManager.saveUsername("roundtrip_user")
        assertEquals("roundtrip_user", preferencesMap["github_username"])
    }

    @Test
    fun `saveLanguageMode then read via preferencesMap`() = runTest {
        tokenManager.saveLanguageMode("ENGLISH")
        assertEquals("ENGLISH", preferencesMap["language_mode"])
    }

    @Test
    fun `saveThemeMode then read via preferencesMap`() = runTest {
        tokenManager.saveThemeMode("LIGHT")
        assertEquals("LIGHT", preferencesMap["theme_mode"])
    }

    @Test
    fun `saveCache stores value correctly`() = runTest {
        tokenManager.saveCache("repos", "[{\"id\":1}]")
        assertEquals("[{\"id\":1}]", preferencesMap["cache_repos"])
    }

    @Test
    fun `clearCache removes specific key`() = runTest {
        tokenManager.saveCache("user", "{\"login\":\"test\"}")
        assertTrue(preferencesMap.containsKey("cache_user"))

        tokenManager.clearCache("user")
        assertFalse(preferencesMap.containsKey("cache_user"))
    }

    @Test
    fun `clearAll removes token username and cache keys`() = runTest {
        tokenManager.saveToken("ghp_test")
        tokenManager.saveUsername("user")
        tokenManager.saveCache("repos", "[]")

        tokenManager.clearAll()

        assertFalse(preferencesMap.containsKey("github_token"))
        assertFalse(preferencesMap.containsKey("github_username"))
        assertFalse(preferencesMap.containsKey("cache_user"))
        assertFalse(preferencesMap.containsKey("cache_repos"))
        assertFalse(preferencesMap.containsKey("cache_starred"))
        assertFalse(preferencesMap.containsKey("cache_profile"))
    }

    @Test
    fun `clearAllCache removes only cache keys`() = runTest {
        tokenManager.saveToken("ghp_test")
        tokenManager.saveUsername("user")
        tokenManager.saveCache("repos", "[]")

        tokenManager.clearAllCache()

        assertTrue(preferencesMap.containsKey("github_token"))
        assertTrue(preferencesMap.containsKey("github_username"))
        assertFalse(preferencesMap.containsKey("cache_user"))
        assertFalse(preferencesMap.containsKey("cache_repos"))
        assertFalse(preferencesMap.containsKey("cache_starred"))
        assertFalse(preferencesMap.containsKey("cache_profile"))
    }
}
