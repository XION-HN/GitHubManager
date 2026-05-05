package com.github.manager.ui.screens.repo

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.manager.data.model.*
import com.github.manager.ui.i18n.*

@Composable
fun CommitsList(
    commits: List<Commit>,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    onLoadMore: () -> Unit = {}
) {
    if (commits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BilingualLabelSmall(I18nStrings.noData)
        }
        return
    }
    val listState = rememberLazyListState()
    val isAtEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(isAtEnd) {
        if (isAtEnd && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }
    LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(commits) { commit ->
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = commit.commit.message, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = commit.commit.author.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = commit.sha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = commit.commit.author.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun IssuesList(
    issues: List<Issue>,
    stateFilter: String,
    onStateFilterChange: (String) -> Unit,
    onCreateIssue: () -> Unit,
    onCloseIssue: (Int) -> Unit,
    onReopenIssue: (Int) -> Unit,
    onViewComments: (Int) -> Unit,
    onAddComment: (Int) -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    onLoadMore: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("open", "closed", "all").forEach { state ->
                FilterChip(
                    selected = stateFilter == state,
                    onClick = { onStateFilterChange(state) },
                    label = {
                        val txt = when (state) {
                            "open" -> getText(I18nStrings.open)
                            "closed" -> getText(I18nStrings.closed)
                            else -> getText(I18nStrings.all)
                        }
                        Text(txt, style = MaterialTheme.typography.bodySmall)
                    }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            val listState = rememberLazyListState()
            val isAtEnd by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= listState.layoutInfo.totalItemsCount - 2
                }
            }
            LaunchedEffect(isAtEnd) {
                if (isAtEnd && hasMore && !isLoadingMore) {
                    onLoadMore()
                }
            }
            LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(issues) { issue ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (issue.state == "open") Icons.Default.BugReport else Icons.Default.CheckCircle,
                                    contentDescription = issue.state,
                                    tint = if (issue.state == "open") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "#${issue.number} ${issue.title}", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                issue.labels?.forEach { label ->
                                    SuggestionChip(onClick = {}, label = { Text(label.name, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (issue.state == "open") {
                                    TextButton(onClick = { onCloseIssue(issue.number) }) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getText(I18nStrings.closeIssue), style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    TextButton(onClick = { onReopenIssue(issue.number) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(getText(I18nStrings.reopenIssue), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                TextButton(onClick = { onViewComments(issue.number) }) {
                                    Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(getText(I18nStrings.comments), style = MaterialTheme.typography.labelSmall)
                                }
                    }
                }
            }
            }
            if (isLoadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        FloatingActionButton(onClick = onCreateIssue, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, contentDescription = getText(I18nStrings.createIssue))
        }
    }
}
}

@Composable
fun PullRequestsList(
    prs: List<PullRequest>,
    stateFilter: String,
    onStateFilterChange: (String) -> Unit,
    onMerge: (Int) -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    onLoadMore: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("open", "closed", "all").forEach { state ->
                FilterChip(
                    selected = stateFilter == state,
                    onClick = { onStateFilterChange(state) },
                    label = {
                        val txt = when (state) {
                            "open" -> getText(I18nStrings.open)
                            "closed" -> getText(I18nStrings.closed)
                            else -> getText(I18nStrings.all)
                        }
                        Text(txt, style = MaterialTheme.typography.bodySmall)
                    }
                )
            }
        }
        val listState = rememberLazyListState()
        val isAtEnd by remember {
            derivedStateOf {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= listState.layoutInfo.totalItemsCount - 2
            }
        }
        LaunchedEffect(isAtEnd) {
            if (isAtEnd && hasMore && !isLoadingMore) {
                onLoadMore()
            }
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(prs) { pr ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when {
                                pr.mergedAt != null -> Icons.Default.CallMerge
                                pr.state == "open" -> Icons.Default.OpenInNew
                                else -> Icons.Default.Close
                            }
                            val color = when {
                                pr.mergedAt != null -> MaterialTheme.colorScheme.primary
                                pr.state == "open" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                            Icon(icon, contentDescription = pr.state, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "#${pr.number} ${pr.title}", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "${pr.head.ref} -> ${pr.base.ref}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (pr.draft) {
                                SuggestionChip(onClick = {}, label = { Text(getText(I18nStrings.draft), style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        if (pr.state == "open" && pr.mergeable == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { onMerge(pr.number) }) {
                                Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(getText(I18nStrings.mergePR), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
        }
        }
        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    }
}

@Composable
fun BranchesList(branches: List<Branch>, currentBranch: String, onSwitchBranch: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(branches) { branch ->
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp).clickable { onSwitchBranch(branch.name) }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, tint = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(branch.name, style = MaterialTheme.typography.bodyMedium, color = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (branch.name == currentBranch) FontWeight.Bold else FontWeight.Normal)
                        Text(branch.commit.sha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (branch.name == currentBranch) { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                    if (branch.protected) { Icon(Icons.Default.Lock, contentDescription = getText(I18nStrings.protected), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)) }
                }
            }
        }
    }
}

@Composable
fun FileBrowserTab(
    files: List<RepoContent>,
    currentPath: String,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onFileClick: (RepoContent) -> Unit,
    readmeContent: String?,
    onLoadReadme: () -> Unit
) {
    var showReadme by remember { mutableStateOf(false) }

    if (showReadme) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showReadme = false }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                BilingualLabelSmall(I18nStrings.readme)
            }
            if (readmeContent == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item { Text(readmeContent, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (canNavigateUp) {
                    IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Up") }
                }
                Text(currentPath.ifBlank { "/" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { onLoadReadme(); showReadme = true }) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(getText(I18nStrings.readme), style = MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider()
            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(files) { file ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onFileClick(file) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.type == "dir") Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = file.type,
                            tint = if (file.type == "dir") Color(0xFF54AEFF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, style = MaterialTheme.typography.bodyMedium)
                            if (file.size > 0 && file.type != "dir") {
                                Text(formatFileSize(file.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (file.type == "dir") { Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

internal fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${"%.1f".format(size / 1024.0)} KB"
        else -> "${"%.1f".format(size / (1024.0 * 1024.0))} MB"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSelectDialog(branches: List<Branch>, currentBranch: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { BilingualLabel(I18nStrings.selectBranch) }, text = {
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            items(branches) { branch ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(branch.name) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(branch.name, fontWeight = if (branch.name == currentBranch) FontWeight.Bold else FontWeight.Normal, color = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    if (branch.name == currentBranch) { Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(getText(I18nStrings.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsTab(
    workflows: List<Workflow>,
    runs: List<WorkflowRun>,
    onRefresh: () -> Unit,
    onTriggerWorkflow: () -> Unit,
    onReRun: (Long) -> Unit,
    onCancelRun: (Long) -> Unit,
    isMonitoring: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BilingualLabelSmall(I18nStrings.actions)
                if (isMonitoring) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(getText(I18nStrings.monitoring), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row {
                if (workflows.isNotEmpty()) {
                    IconButton(onClick = onTriggerWorkflow) { Icon(Icons.Default.PlayArrow, contentDescription = getText(I18nStrings.triggerWorkflow)) }
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = getText(I18nStrings.refresh)) }
            }
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (runs.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { BilingualLabelSmall(I18nStrings.noRuns) } }
            }
            items(runs) { run -> WorkflowRunCard(run = run, onReRun = { onReRun(run.id) }, onCancel = { onCancelRun(run.id) }) }
        }
    }
}

@Composable
fun WorkflowRunCard(run: WorkflowRun, onReRun: () -> Unit, onCancel: () -> Unit) {
    val (icon, color) = when (run.conclusion) {
        "success" -> Icons.Default.CheckCircle to Color(0xFF2ea44f)
        "failure" -> Icons.Default.Cancel to Color(0xFFd73a49)
        "cancelled" -> Icons.Default.StopCircle to Color(0xFFd29922)
        null -> when (run.status) {
            "in_progress" -> Icons.Default.Sync to Color(0xFF0366d6)
            "queued" -> Icons.Default.Schedule to Color(0xFFd29922)
            "waiting" -> Icons.Default.HourglassEmpty to Color(0xFFd29922)
            else -> Icons.Default.PlayCircleFilled to Color(0xFF0366d6)
        }
        else -> Icons.Default.Help to Color.Gray
    }
    val statusText = when (run.conclusion) {
        "success" -> getText(I18nStrings.success)
        "failure" -> getText(I18nStrings.failed)
        "cancelled" -> getText(I18nStrings.cancelled)
        null -> when (run.status) {
            "in_progress" -> getText(I18nStrings.inProgress)
            "queued" -> getText(I18nStrings.queued)
            "waiting" -> getText(I18nStrings.waiting)
            else -> run.status
        }
        else -> run.conclusion ?: ""
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (run.status == "in_progress") { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = color) }
                    else { Icon(icon, contentDescription = statusText, tint = color, modifier = Modifier.size(16.dp)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(run.name.ifBlank { "${getText(I18nStrings.runNumber)}${run.id}" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Row {
                    if (run.status == "in_progress" || run.status == "queued" || run.status == "waiting") {
                        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Stop, contentDescription = getText(I18nStrings.cancelRun), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error) }
                    } else {
                        IconButton(onClick = onReRun, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Replay, contentDescription = getText(I18nStrings.reRun), modifier = Modifier.size(14.dp)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(4.dp)); Text(run.headBranch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text(run.headSha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(4.dp)); Text(safeSubstring(run.createdAt, 0, 10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text(statusText, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun ReleasesList(releases: List<Release>) {
    if (releases.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BilingualLabelSmall(I18nStrings.noReleases) }
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(releases) { release ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(release.tagName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            if (release.prerelease) {
                                Spacer(modifier = Modifier.width(8.dp))
                                SuggestionChip(onClick = {}, label = { Text("pre", style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        release.name?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        release.body?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(safeSubstring(release.publishedAt ?: release.createdAt, 0, 10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!release.assets.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(getText(I18nStrings.assets), style = MaterialTheme.typography.labelMedium)
                            release.assets?.forEach { asset ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(asset.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(formatFileSize(asset.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerWorkflowDialog(
    workflows: List<Workflow>,
    defaultBranch: String,
    onTrigger: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedWorkflowId by remember { mutableStateOf(workflows.firstOrNull()?.id ?: 0L) }
    var ref by remember { mutableStateOf(defaultBranch) }

    AlertDialog(onDismissRequest = onDismiss, title = { BilingualLabel(I18nStrings.triggerWorkflowDialog) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(getText(I18nStrings.selectWorkflow), style = MaterialTheme.typography.labelMedium)
            workflows.forEach { workflow ->
                Row(modifier = Modifier.fillMaxWidth().clickable { selectedWorkflowId = workflow.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedWorkflowId == workflow.id, onClick = { selectedWorkflowId = workflow.id })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(workflow.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text(getText(I18nStrings.branchRef)) }, singleLine = true)
        }
    }, confirmButton = {
        TextButton(onClick = { onTrigger(selectedWorkflowId, ref) }, enabled = selectedWorkflowId > 0 && ref.isNotBlank()) { Text(getText(I18nStrings.trigger)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(getText(I18nStrings.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommentDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var body by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { BilingualLabel(I18nStrings.addComment) }, text = {
        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(getText(I18nStrings.comment)) }, maxLines = 5)
    }, confirmButton = {
        TextButton(onClick = { onAdd(body); body = "" }, enabled = body.isNotBlank()) { Text(getText(I18nStrings.comment)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(getText(I18nStrings.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsDialog(comments: List<IssueComment>, onDismiss: () -> Unit, onAddComment: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { BilingualLabel(I18nStrings.comments) }, text = {
        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
            if (comments.isEmpty()) {
                item { Text(getText(I18nStrings.noData), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(comments) { comment ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.user?.login ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(safeSubstring(comment.createdAt, 0, 10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(comment.body, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }, confirmButton = {
        Row {
            TextButton(onClick = onAddComment) { Text(getText(I18nStrings.addComment)) }
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    })
}

@Composable
fun CreateIssueDialog(onDismiss: () -> Unit, onCreate: (title: String, body: String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { BilingualLabel(I18nStrings.createIssue) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(getText(I18nStrings.title)) }, singleLine = true)
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(getText(I18nStrings.bodyOptional)) }, maxLines = 5)
        }
    }, confirmButton = {
        TextButton(onClick = { onCreate(title, body.ifBlank { null }) }, enabled = title.isNotBlank()) { Text(getText(I18nStrings.create)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(getText(I18nStrings.cancel)) } })
}
