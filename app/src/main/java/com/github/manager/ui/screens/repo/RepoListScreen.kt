package com.github.manager.ui.screens.repo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.github.manager.data.model.Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    onRepoClick: (owner: String, repo: String) -> Unit,
    onLogout: () -> Unit,
    viewModel: RepoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        uiState.user?.avatar_url?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(uiState.user?.login ?: "GitHub Manager")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Repo")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = if (uiState.isStarredTab) 1 else 0) {
                Tab(
                    selected = !uiState.isStarredTab,
                    onClick = { viewModel.toggleTab(false) },
                    text = { Text("My Repos") }
                )
                Tab(
                    selected = uiState.isStarredTab,
                    onClick = { viewModel.toggleTab(true) },
                    text = { Text("Starred") }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = viewModel::loadRepos) { Text("Retry") }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.repos) { repo ->
                        RepoItem(
                            repo = repo,
                            onClick = { onRepoClick(repo.owner.login, repo.name) },
                            onStarClick = {
                                viewModel.starRepo(repo.owner.login, repo.name)
                            },
                            onForkClick = {
                                viewModel.forkRepo(repo.owner.login, repo.name)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRepoDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, isPrivate ->
                viewModel.createRepo(name, desc, isPrivate)
                showCreateDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RepoItem(
    repo: Repository,
    onClick: () -> Unit,
    onStarClick: () -> Unit,
    onForkClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repo.full_name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (repo.private) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Private", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            repo.description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repo.language?.let { lang ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(lang, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.stargazers_count}", style = MaterialTheme.typography.labelSmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.forks_count}", style = MaterialTheme.typography.labelSmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.open_issues_count}", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (repo.topics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repo.topics.take(3).forEach { topic ->
                        SuggestionChip(onClick = {}, label = { Text(topic, style = MaterialTheme.typography.labelSmall) })
                    }
                    if (repo.topics.size > 3) {
                        SuggestionChip(onClick = {}, label = { Text("+${repo.topics.size - 3}", style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        }
    }
}

@Composable
fun CreateRepoDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, isPrivate: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Repository") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Repository Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Private")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description.ifBlank { null }, isPrivate) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
