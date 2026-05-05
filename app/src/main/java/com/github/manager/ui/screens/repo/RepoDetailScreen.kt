package com.github.manager.ui.screens.repo

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.manager.data.model.*
import com.github.manager.ui.i18n.*
import kotlinx.coroutines.delay

internal fun safeSubstring(s: String, start: Int, end: Int): String {
    return try { s.substring(start, end) } catch (e: Exception) { s }
}

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
    var showTriggerWorkflowDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableIntStateOf(-1) }
    var showCommentsDialog by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

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

    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage != null) {
            delay(2000)
            viewModel.clearActionMessage()
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
                            contentDescription = getText(if (uiState.isStarred) I18nStrings.unstar else I18nStrings.star),
                            tint = if (uiState.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.forkRepo() }) {
                        Icon(Icons.Default.CallSplit, contentDescription = getText(I18nStrings.fork))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = getText(I18nStrings.delete), tint = MaterialTheme.colorScheme.error)
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
                            modifier = Modifier.clickable(role = Role.Button) { showBranchDialog = true }
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
                HorizontalDivider()
            }

            uiState.actionMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = { TextButton(onClick = { viewModel.clearActionMessage() }) { Text("OK") } }
                ) { Text(msg, style = MaterialTheme.typography.bodySmall) }
            }

            val tabs = listOf(
                getText(I18nStrings.commits) to "Commits",
                getText(I18nStrings.issues) to "Issues",
                getText(I18nStrings.pullRequests) to "PRs",
                getText(I18nStrings.branches) to "Branches",
                getText(I18nStrings.files) to "Files",
                getText(I18nStrings.actions) to "Actions",
                getText(I18nStrings.releases) to "Releases"
            )
            ScrollableTabRow(
                selectedTabIndex = if (uiState.currentTab >= tabs.size) 0 else uiState.currentTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, (label, enLabel) ->
                    Tab(
                        selected = uiState.currentTab == index,
                        onClick = { viewModel.onTabChanged(index) },
                        text = {
                            when (languageModeState.value) {
                                LanguageMode.CHINESE -> Text(label)
                                LanguageMode.ENGLISH -> Text(enLabel)
                                LanguageMode.BILINGUAL -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, fontSize = 11.sp)
                                    Text(enLabel, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 8.sp)
                                }
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading && !uiState.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                if (pullToRefreshState.isAnimating) {
                    LaunchedEffect(true) {
                        viewModel.refresh()
                    }
                }
                LaunchedEffect(uiState.isRefreshing) {
                    if (!uiState.isRefreshing) {
                        pullToRefreshState.endRefresh()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullToRefreshState.nestedScrollConnection)
                ) {
                    AnimatedContent(
                        targetState = uiState.currentTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300)) with
                                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(tween(300))
                            } else {
                                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300)) with
                                slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(300)) + fadeOut(tween(300))
                            }
                        }
                    ) { tab ->
                        when (tab) {
                            0 -> CommitsList(
                                commits = uiState.commits,
                                isLoadingMore = uiState.isLoadingMore,
                                hasMore = uiState.hasMoreCommits,
                                onLoadMore = { viewModel.loadMoreCommits() }
                            )
                            1 -> IssuesList(
                                issues = uiState.issues,
                                stateFilter = uiState.issueStateFilter,
                                onStateFilterChange = { viewModel.setIssueFilter(it) },
                                onCreateIssue = { showCreateIssueDialog = true },
                                onCloseIssue = { viewModel.closeIssue(it) },
                                onReopenIssue = { viewModel.reopenIssue(it) },
                                onViewComments = { showCommentsDialog = it },
                                onAddComment = { showCommentDialog = it },
                                isLoadingMore = uiState.isLoadingMore,
                                hasMore = uiState.hasMoreIssues,
                                onLoadMore = { viewModel.loadMoreIssues() }
                            )
                            2 -> PullRequestsList(
                                prs = uiState.pullRequests,
                                stateFilter = uiState.prStateFilter,
                                onStateFilterChange = { viewModel.setPrFilter(it) },
                                onMerge = { viewModel.mergePullRequest(it) },
                                isLoadingMore = uiState.isLoadingMore,
                                hasMore = uiState.hasMorePrs,
                                onLoadMore = { viewModel.loadMorePullRequests() }
                            )
                            3 -> BranchesList(
                                branches = uiState.branches,
                                currentBranch = uiState.currentBranch ?: uiState.repo?.defaultBranch ?: "main",
                                onSwitchBranch = { viewModel.switchBranch(it) }
                            )
                            4 -> FileBrowserTab(
                                files = uiState.files,
                                currentPath = uiState.currentPath,
                                canNavigateUp = uiState.pathStack.isNotEmpty(),
                                onNavigateUp = { viewModel.navigateUp() },
                                onFileClick = { file ->
                                    if (file.type == "dir") {
                                        viewModel.loadFiles(file.path)
                                    } else {
                                        file.htmlUrl.takeIf { it.isNotBlank() }?.let {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                                        }
                                    }
                                },
                                readmeContent = uiState.readmeContent,
                                onLoadReadme = { viewModel.loadReadme() }
                            )
                            5 -> ActionsTab(
                                workflows = uiState.workflows,
                                runs = uiState.workflowRuns,
                                onRefresh = { viewModel.loadWorkflowRuns() },
                                onTriggerWorkflow = { showTriggerWorkflowDialog = true },
                                onReRun = { viewModel.reRunWorkflow(it) },
                                onCancelRun = { viewModel.cancelWorkflowRun(it) },
                                isMonitoring = uiState.isMonitoringActions
                            )
                            6 -> ReleasesList(uiState.releases)
                        }
                    }
                    PullToRefreshContainer(
                        state = pullToRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { BilingualLabel(I18nStrings.deleteRepo) },
            text = { Text(getText(I18nStrings.deleteRepoConfirm)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteRepo(); showDeleteDialog = false; onBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(getText(I18nStrings.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(getText(I18nStrings.cancel)) } }
        )
    }

    if (showCreateIssueDialog) {
        CreateIssueDialog(
            onDismiss = { showCreateIssueDialog = false },
            onCreate = { t, b -> viewModel.createIssue(t, b); showCreateIssueDialog = false }
        )
    }

    if (showBranchDialog) {
        BranchSelectDialog(
            branches = uiState.branches,
            currentBranch = uiState.currentBranch ?: uiState.repo?.defaultBranch ?: "main",
            onSelect = { viewModel.switchBranch(it); showBranchDialog = false },
            onDismiss = { showBranchDialog = false }
        )
    }

    if (showTriggerWorkflowDialog) {
        TriggerWorkflowDialog(
            workflows = uiState.workflows,
            defaultBranch = uiState.currentBranch ?: uiState.repo?.defaultBranch ?: "main",
            onTrigger = { id, ref -> viewModel.dispatchWorkflow(id, ref); showTriggerWorkflowDialog = false },
            onDismiss = { showTriggerWorkflowDialog = false }
        )
    }

    if (showCommentDialog >= 0) {
        AddCommentDialog(
            onDismiss = { showCommentDialog = -1 },
            onAdd = { body -> viewModel.addComment(body); showCommentDialog = -1 }
        )
    }

    if (showCommentsDialog >= 0) {
        LaunchedEffect(showCommentsDialog) { viewModel.loadIssueComments(showCommentsDialog) }
        CommentsDialog(
            comments = uiState.issueComments,
            onDismiss = { showCommentsDialog = -1 },
            onAddComment = { showCommentDialog = showCommentsDialog; showCommentsDialog = -1 }
        )
    }
}
