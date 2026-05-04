package com.github.manager.ui.screens.repo

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.github.manager.data.model.Repository
import com.github.manager.ui.i18n.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    onRepoClick: (owner: String, repo: String) -> Unit,
    onAccountClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: RepoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = onAccountClick
                        )
                    ) {
                        uiState.user?.avatarUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(uiState.user?.login ?: bt(Strings.appName))
                            if (languageModeState.value == LanguageMode.BILINGUAL && uiState.user != null) {
                                Text(
                                    "My Repositories",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = bt(Strings.createRepo))
                    }
                    IconButton(onClick = onAccountClick) {
                        uiState.user?.avatarUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = bt(Strings.account),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        } ?: run {
                            Icon(Icons.Default.AccountCircle, contentDescription = bt(Strings.account))
                        }
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
                    text = { BilingualLabelSmall(Strings.myRepos) }
                )
                Tab(
                    selected = uiState.isStarredTab,
                    onClick = { viewModel.toggleTab(true) },
                    text = { BilingualLabelSmall(Strings.starred) }
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
                        Button(onClick = viewModel::loadRepos) { Text(bt(Strings.retry)) }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.repos, key = { it.id }) { repo ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
                            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 3 }
                        ) {
                            RepoItem(
                                repo = repo,
                                isStarred = uiState.starredRepos.contains(repo.fullName),
                                onClick = { onRepoClick(repo.owner.login, repo.name) },
                                onStarClick = {
                                    if (uiState.starredRepos.contains(repo.fullName)) {
                                        viewModel.unstarRepo(repo.owner.login, repo.name)
                                    } else {
                                        viewModel.starRepo(repo.owner.login, repo.name)
                                    }
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
}

@Composable
fun RepoItem(
    repo: Repository,
    isStarred: Boolean,
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
                    text = repo.fullName,
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
                        label = { Text(bt(Strings.privateRepo), style = MaterialTheme.typography.labelSmall) },
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onStarClick)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = bt(Strings.star),
                        modifier = Modifier.size(14.dp),
                        tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.forksCount}", style = MaterialTheme.typography.labelSmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.openIssuesCount}", style = MaterialTheme.typography.labelSmall)
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
        title = { BilingualLabel(Strings.createRepository) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(bt(Strings.repositoryName)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(bt(Strings.descriptionOptional)) },
                    maxLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bt(Strings.privateRepo))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description.ifBlank { null }, isPrivate) },
                enabled = name.isNotBlank()
            ) { Text(bt(Strings.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(bt(Strings.cancel)) }
        }
    )
}
