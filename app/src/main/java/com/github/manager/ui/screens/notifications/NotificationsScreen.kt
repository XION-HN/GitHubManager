package com.github.manager.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.manager.data.model.Notification
import com.github.manager.ui.components.PullToRefreshBox
import com.github.manager.ui.components.rememberCustomPullToRefreshState
import com.github.manager.ui.i18n.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onRepoClick: (owner: String, repo: String) -> Unit = { _, _ -> },
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMarkAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadNotifications() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { BilingualLabel(I18nStrings.notifications) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMarkAllDialog = true }) {
                        Icon(Icons.Default.DoneAll, contentDescription = getText(I18nStrings.markAllRead))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.notifications.isEmpty() && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BilingualBody(I18nStrings.noNotifications)
                }
            }
        } else {
            val pullToRefreshState = rememberCustomPullToRefreshState()
            val listState = rememberLazyListState()

            PullToRefreshBox(
                state = pullToRefreshState,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                LaunchedEffect(uiState.isRefreshing) {
                    if (!uiState.isRefreshing) pullToRefreshState.endRefresh()
                }

                LazyColumn(state = listState) {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onMarkRead = { viewModel.markAsRead(notification.id) },
                            onClick = {
                                val parts = notification.repository.fullName.split("/")
                                if (parts.size == 2) {
                                    onRepoClick(parts[0], parts[1])
                                }
                            }
                        )
                    }

                    if (uiState.hasMore) {
                        item {
                            LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
                                if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != null &&
                                    listState.layoutInfo.visibleItemsInfo.last().index >= uiState.notifications.size - 3) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { viewModel.loadNotifications() }) { Text(getText(I18nStrings.retry)) } }
            ) { Text(error) }
        }
    }

    if (showMarkAllDialog) {
        AlertDialog(
            onDismissRequest = { showMarkAllDialog = false },
            title = { BilingualLabel(I18nStrings.markAllRead) },
            text = { Text(getText(I18nStrings.markAllRead)) },
            confirmButton = {
                TextButton(onClick = { viewModel.markAllAsRead(); showMarkAllDialog = false }) {
                    Text(getText(I18nStrings.markAllRead))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAllDialog = false }) {
                    Text(getText(I18nStrings.cancel))
                }
            }
        )
    }
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onMarkRead: () -> Unit,
    onClick: () -> Unit
) {
    val typeIcon = when (notification.subject.type) {
        "Issue" -> Icons.Default.ErrorOutline
        "PullRequest" -> Icons.Default.CallSplit
        "Commit" -> Icons.Default.Commit
        "Release" -> Icons.Default.NewReleases
        "Discussion" -> Icons.Default.Forum
        else -> Icons.Default.Notifications
    }

    val reasonLabel = when (notification.reason) {
        "subscribe" -> "subscribed"
        "manual" -> "manual"
        "author" -> "author"
        "comment" -> "comment"
        "mention" -> "mentioned"
        "team_mention" -> "team mentioned"
        "review_requested" -> "review requested"
        "assign" -> "assigned"
        "invitation" -> "invited"
        else -> notification.reason
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.unread)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                typeIcon,
                contentDescription = notification.subject.type,
                modifier = Modifier.size(20.dp),
                tint = if (notification.unread)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.subject.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (notification.unread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.repository.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        reasonLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (notification.unread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                getText(I18nStrings.unread),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            if (notification.unread) {
                IconButton(onClick = onMarkRead, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.CheckCircleOutline,
                        contentDescription = getText(I18nStrings.markRead),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
