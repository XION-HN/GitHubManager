package com.github.manager.ui.screens.account

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.github.manager.ui.i18n.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSwitchTokenDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var newToken by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { BilingualLabel(I18nStrings.account) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
        actions = {
                IconButton(onClick = { showThemeDialog = true }) {
                    Icon(
                        imageVector = if (themeModeState.value == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = getText(I18nStrings.theme)
                    )
                }
                IconButton(onClick = { showLanguageDialog = true }) {
                    Icon(Icons.Default.Language, contentDescription = getText(I18nStrings.language))
                }
            }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = viewModel::loadProfile) { Text(getText(I18nStrings.retry)) }
                    }
                }
            }
            uiState.user != null -> {
                val user = uiState.user!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user.name ?: user.login,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "@${user.login}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    user.bio?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(I18nStrings.repos, user.publicRepos)
                        StatItem(I18nStrings.gists, user.publicGists)
                        StatItem(I18nStrings.followers, user.followers)
                        StatItem(I18nStrings.following, user.following)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (user.htmlUrl.isNotBlank()) {
                ListItem(
                    headlineContent = { BilingualLabelSmall(I18nStrings.githubProfile) },
                    leadingContent = {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    },
                    trailingContent = {
                        Text(user.htmlUrl, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp))
                    },
                    modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(user.htmlUrl)))
                    }
                )
            }

                        ListItem(
                            headlineContent = { BilingualLabelSmall(I18nStrings.memberSince) },
                            leadingContent = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            },
                            trailingContent = {
                                Text(formatDate(user.createdAt), style = MaterialTheme.typography.bodySmall)
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (themeModeState.value == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getText(I18nStrings.theme))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                            Icon(Icons.Default.Language, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getText(I18nStrings.language))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showSwitchTokenDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getText(I18nStrings.switchAccount))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getText(I18nStrings.logout))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { BilingualLabel(I18nStrings.logout) },
            text = { Text(getText(I18nStrings.logoutConfirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(getText(I18nStrings.logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(getText(I18nStrings.cancel)) }
            }
        )
    }

    if (showSwitchTokenDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchTokenDialog = false },
            title = { BilingualLabel(I18nStrings.switchAccount) },
            text = {
                OutlinedTextField(
                    value = newToken,
                    onValueChange = { newToken = it },
                    label = { Text(getText(I18nStrings.newPersonalAccessToken)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newToken.isNotBlank()) {
                            viewModel.switchAccount(newToken)
                            showSwitchTokenDialog = false
                            newToken = ""
                        }
                    },
                    enabled = newToken.isNotBlank()
                ) { Text(getText(I18nStrings.switchAccountAction)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSwitchTokenDialog = false
                    newToken = ""
                }) { Text(getText(I18nStrings.cancel)) }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { BilingualLabel(I18nStrings.language) },
            text = {
                Column {
                    LanguageOption(
                        label = I18nStrings.bilingual.zh,
                        subtitle = I18nStrings.bilingual.en,
                        selected = languageModeState.value == LanguageMode.BILINGUAL,
                        onClick = {
                            languageModeState.value = LanguageMode.BILINGUAL
                            viewModel.saveLanguageMode("BILINGUAL")
                        }
                    )
                    LanguageOption(
                        label = I18nStrings.chinese.zh,
                        selected = languageModeState.value == LanguageMode.CHINESE,
                        onClick = {
                            languageModeState.value = LanguageMode.CHINESE
                            viewModel.saveLanguageMode("CHINESE")
                        }
                    )
                    LanguageOption(
                        label = I18nStrings.english.en,
                        selected = languageModeState.value == LanguageMode.ENGLISH,
                        onClick = {
                            languageModeState.value = LanguageMode.ENGLISH
                            viewModel.saveLanguageMode("ENGLISH")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("OK") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { BilingualLabel(I18nStrings.theme) },
            text = {
                Column {
                    LanguageOption(
                        label = I18nStrings.systemDefault.zh,
                        subtitle = I18nStrings.systemDefault.en,
                        selected = themeModeState.value == ThemeMode.SYSTEM,
                        onClick = {
                            themeModeState.value = ThemeMode.SYSTEM
                            viewModel.saveThemeMode("SYSTEM")
                        }
                    )
                    LanguageOption(
                        label = I18nStrings.lightMode.zh,
                        subtitle = I18nStrings.lightMode.en,
                        selected = themeModeState.value == ThemeMode.LIGHT,
                        onClick = {
                            themeModeState.value = ThemeMode.LIGHT
                            viewModel.saveThemeMode("LIGHT")
                        }
                    )
                    LanguageOption(
                        label = I18nStrings.darkMode.zh,
                        subtitle = I18nStrings.darkMode.en,
                        selected = themeModeState.value == ThemeMode.DARK,
                        onClick = {
                            themeModeState.value = ThemeMode.DARK
                            viewModel.saveThemeMode("DARK")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun LanguageOption(
    label: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            subtitle?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatItem(text: BilingualText, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge
        )
        BilingualLabelSmall(text)
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        dateStr.substring(0, 10)
    } catch (e: Exception) {
        dateStr
    }
}
