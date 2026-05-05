package com.github.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.github.manager.data.local.TokenManager
import com.github.manager.ui.i18n.LanguageMode
import com.github.manager.ui.i18n.ThemeMode
import com.github.manager.ui.i18n.languageModeState
import com.github.manager.ui.i18n.themeModeState
import com.github.manager.ui.navigation.GitHubNavHost
import com.github.manager.ui.theme.GitHubManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        loadPreferences()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitHubManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GitHubNavHost()
                }
            }
        }
    }

    private fun loadPreferences() {
        lifecycleScope.launch(Dispatchers.IO) {
            val langMode = tokenManager.loadLanguageMode()
            val themeMode = tokenManager.loadThemeMode()
            withContext(Dispatchers.Main) {
                langMode?.let { mode ->
                    languageModeState.value = when (mode) {
                        "CHINESE" -> LanguageMode.CHINESE
                        "ENGLISH" -> LanguageMode.ENGLISH
                        else -> LanguageMode.BILINGUAL
                    }
                }
                themeMode?.let { mode ->
                    themeModeState.value = when (mode) {
                        "LIGHT" -> ThemeMode.LIGHT
                        "DARK" -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                }
            }
        }
    }
}
