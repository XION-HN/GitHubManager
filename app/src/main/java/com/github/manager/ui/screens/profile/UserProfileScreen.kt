package com.github.manager.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.ui.i18n.*

data class UserProfileUiState(
    val user: User? = null,
    val repos: List<Repository> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    username: String,
    onBack: () -> Unit,
    onRepoClick: (owner: String, repo: String) -> Unit = { _, _ -> },
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(username) { viewModel.loadUser(username) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(username, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.user?.let { user ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.login,
                                modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.large)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(user.name ?: user.login, style = MaterialTheme.typography.titleLarge)
                            Text("@${user.login}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            user.bio?.let { bio ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                StatItem(getText(I18nStrings.repos), user.publicRepos)
                                StatItem(getText(I18nStrings.followers), user.followers)
                                StatItem(getText(I18nStrings.following), user.following)
                                StatItem(getText(I18nStrings.gists), user.publicGists)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(user.htmlUrl)))
                                }
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(getText(I18nStrings.viewOnGithub))
                            }
                        }
                        Divider()
                    }

                    item {
                        Text(
                            getText(I18nStrings.userRepos),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(uiState.repos, key = { it.id }) { repo ->
                        UserRepoItem(repo = repo, onClick = { onRepoClick(repo.owner.login, repo.name) })
                    }

                    if (uiState.hasMore && uiState.repos.isNotEmpty()) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMoreRepos() }
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UserRepoItem(repo: Repository, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (repo.private) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(repo.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            repo.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repo.language?.let { lang ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(lang, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${repo.forksCount}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
