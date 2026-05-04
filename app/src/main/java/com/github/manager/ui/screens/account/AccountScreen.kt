package com.github.manager.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

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
    var newToken by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
        TopAppBar(
            title = { Text("Account") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = viewModel::loadProfile) { Text("Retry") }
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
                        StatItem("Repos", user.publicRepos)
                        StatItem("Gists", user.publicGists)
                        StatItem("Followers", user.followers)
                        StatItem("Following", user.following)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (user.htmlUrl.isNotBlank()) {
                            ListItem(
                                headlineContent = { Text("GitHub Profile") },
                                leadingContent = {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                },
                                trailingContent = {
                                    Text(user.htmlUrl, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp))
                                },
                                modifier = Modifier.clip(MaterialTheme.shapes.medium)
                            )
                        }

                        ListItem(
                            headlineContent = { Text("Member Since") },
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
                            onClick = { showSwitchTokenDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch Account")
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
                            Text("Logout")
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
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout? You will need to enter your token again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSwitchTokenDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchTokenDialog = false },
            title = { Text("Switch Account") },
            text = {
                OutlinedTextField(
                    value = newToken,
                    onValueChange = { newToken = it },
                    label = { Text("New Personal Access Token") },
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
                ) { Text("Switch") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSwitchTokenDialog = false
                    newToken = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        dateStr.substring(0, 10)
    } catch (e: Exception) {
        dateStr
    }
}
