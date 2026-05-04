package com.github.manager.ui.screens.repo

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.manager.data.model.Branch
import com.github.manager.data.model.Commit
import com.github.manager.data.model.Issue
import com.github.manager.data.model.PullRequest
import com.github.manager.data.model.Workflow
import com.github.manager.data.model.WorkflowRun
import com.github.manager.ui.i18n.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RepoDetailScreen(
    owner: String,
    repo: String,
    onBack: () -> Unit,
    viewModel: RepoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateIssueDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(owner, repo) {
        viewModel.init(owner, repo)
    }

    LaunchedEffect(uiState.currentTab) {
        if (uiState.currentTab == 5) {
            while (true) {
                delay(15000)
                viewModel.loadWorkflowRuns()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$owner/$repo", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleStar() }) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = getText(if (uiState.isStarred) Strings.unstar else Strings.star),
                            tint = if (uiState.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.forkRepo() }) {
                        Icon(Icons.Default.CallSplit, contentDescription = getText(Strings.fork))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = getText(Strings.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            uiState.repo?.let { repoDetail ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(repoDetail.fullName, style = MaterialTheme.typography.titleLarge)
                    repoDetail.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repoDetail.language?.let { lang ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(lang, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${repoDetail.stargazersCount}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${repoDetail.forksCount}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(role = Role.Button) {
                                showBranchDialog = true
                            }
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                uiState.currentBranch ?: repoDetail.defaultBranch,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Divider()
            }

            val tabs = listOf(
                getText(Strings.commits) to "Commits",
                getText(Strings.issues) to "Issues",
                getText(Strings.pullRequests) to "Pull Requests",
                getText(Strings.branches) to "Branches",
                getText(Strings.actions) to "Actions"
            )
            ScrollableTabRow(
                selectedTabIndex = if (uiState.currentTab > 4) 0 else uiState.currentTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, (label, _) ->
                    Tab(
                        selected = uiState.currentTab == index,
                        onClick = { viewModel.onTabChanged(index) },
                        text = {
                            when (languageModeState.value) {
                                LanguageMode.CHINESE -> Text(label)
                                LanguageMode.ENGLISH -> Text(tabs[index].second)
                                LanguageMode.BILINGUAL -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, fontSize = 12.sp)
                                    Text(
                                        tabs[index].second,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(
                                initialOffsetX = { it / 3 },
                                animationSpec = tween(300)
                            ) + fadeIn(tween(300)) with
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut(tween(300))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeIn(tween(300)) with
                            slideOutHorizontally(
                                targetOffsetX = { it / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut(tween(300))
                        }
                    }
                ) { tab ->
                    when (tab) {
                        0 -> CommitsList(uiState.commits)
                        1 -> IssuesList(
                            issues = uiState.issues,
                            onCreateIssue = { showCreateIssueDialog = true }
                        )
                        2 -> PullRequestsList(uiState.pullRequests)
                        3 -> BranchesList(
                            branches = uiState.branches,
                            currentBranch = uiState.currentBranch ?: uiState.repo?.defaultBranch ?: "main",
                            onSwitchBranch = { viewModel.switchBranch(it) }
                        )
                        4 -> ActionsTab(
                            workflows = uiState.workflows,
                            runs = uiState.workflowRuns,
                            onRefresh = { viewModel.loadWorkflowRuns() },
                            onTriggerWorkflow = { workflowId, ref ->
                                viewModel.dispatchWorkflow(workflowId, ref)
                            },
                            onReRun = { runId -> viewModel.reRunWorkflow(runId) },
                            onCancelRun = { runId -> viewModel.cancelWorkflowRun(runId) },
                            isMonitoring = uiState.isMonitoringActions
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { BilingualLabel(Strings.deleteRepo) },
            text = { Text(getText(Strings.deleteRepoConfirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRepo()
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(getText(Strings.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(getText(Strings.cancel)) }
            }
        )
    }

    if (showCreateIssueDialog) {
        CreateIssueDialog(
            onDismiss = { showCreateIssueDialog = false },
            onCreate = { title, body ->
                viewModel.createIssue(title, body)
                showCreateIssueDialog = false
            }
        )
    }

    if (showBranchDialog) {
        BranchSelectDialog(
            branches = uiState.branches,
            currentBranch = uiState.currentBranch ?: uiState.repo?.defaultBranch ?: "main",
            onSelect = {
                viewModel.switchBranch(it)
                showBranchDialog = false
            },
            onDismiss = { showBranchDialog = false }
        )
    }
}

@Composable
fun CommitsList(commits: List<Commit>) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(commits) { commit ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = commit.commit.message,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = commit.commit.author.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = commit.sha.take(7),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = commit.commit.author.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun IssuesList(
    issues: List<Issue>,
    onCreateIssue: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(issues) { issue ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (issue.state == "open") Icons.Default.BugReport else Icons.Default.CheckCircle,
                                contentDescription = issue.state,
                                tint = if (issue.state == "open") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "#${issue.number} ${issue.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            issue.labels.forEach { label ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(label.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateIssue,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = getText(Strings.createIssue))
        }
    }
}

@Composable
fun PullRequestsList(prs: List<PullRequest>) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(prs) { pr ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (pr.state) {
                            "open" -> Icons.Default.OpenInNew
                            "closed" -> if (pr.mergedAt != null) Icons.Default.CallMerge else Icons.Default.Close
                            else -> Icons.Default.OpenInNew
                        }
                        val color = when {
                            pr.mergedAt != null -> MaterialTheme.colorScheme.primary
                            pr.state == "open" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Icon(icon, contentDescription = pr.state, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#${pr.number} ${pr.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${pr.head.ref} -> ${pr.base.ref}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pr.draft) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(getText(Strings.draft), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BranchesList(
    branches: List<Branch>,
    currentBranch: String,
    onSwitchBranch: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(branches) { branch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clickable { onSwitchBranch(branch.name) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CallSplit,
                        contentDescription = null,
                        tint = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            branch.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (branch.name == currentBranch) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            branch.commit.sha.take(7),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (branch.name == currentBranch) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (branch.protected) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = getText(Strings.protected),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSelectDialog(
    branches: List<Branch>,
    currentBranch: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { BilingualLabel(Strings.selectBranch) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(branches) { branch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(branch.name) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            branch.name,
                            fontWeight = if (branch.name == currentBranch) FontWeight.Bold else FontWeight.Normal,
                            color = if (branch.name == currentBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (branch.name == currentBranch) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(getText(Strings.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsTab(
    workflows: List<Workflow>,
    runs: List<WorkflowRun>,
    onRefresh: () -> Unit,
    onTriggerWorkflow: (Long, String) -> Unit,
    onReRun: (Long) -> Unit,
    onCancelRun: (Long) -> Unit,
    isMonitoring: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BilingualLabelSmall(Strings.actions)
                if (isMonitoring) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        getText(Strings.monitoring),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = getText(Strings.refresh))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (runs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BilingualLabelSmall(Strings.noRuns)
                    }
                }
            }

            items(runs) { run ->
                WorkflowRunCard(
                    run = run,
                    onReRun = { onReRun(run.id) },
                    onCancel = { onCancelRun(run.id) }
                )
            }
        }
    }
}

@Composable
fun WorkflowRunCard(
    run: WorkflowRun,
    onReRun: () -> Unit,
    onCancel: () -> Unit
) {
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
        "success" -> getText(Strings.success)
        "failure" -> getText(Strings.failed)
        "cancelled" -> getText(Strings.cancelled)
        null -> when (run.status) {
            "in_progress" -> getText(Strings.inProgress)
            "queued" -> getText(Strings.queued)
            "waiting" -> getText(Strings.waiting)
            else -> run.status
        }
        else -> run.conclusion ?: ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (run.status == "in_progress") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = color
                        )
                    } else {
                        Icon(icon, contentDescription = statusText, tint = color, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        run.name.ifBlank { "${getText(Strings.runNumber)}${run.id}" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row {
                    if (run.status == "in_progress" || run.status == "queued" || run.status == "waiting") {
                        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = getText(Strings.cancelRun), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = onReRun, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Replay, contentDescription = getText(Strings.reRun), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(run.headBranch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(run.headSha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        run.createdAt.substring(0, 10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CreateIssueDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, body: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { BilingualLabel(Strings.createIssue) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(getText(Strings.title)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(getText(Strings.bodyOptional)) },
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, body.ifBlank { null }) },
                enabled = title.isNotBlank()
            ) { Text(getText(Strings.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(getText(Strings.cancel)) }
        }
    )
}
