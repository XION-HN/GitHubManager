package com.github.manager.ui.screens.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.model.*
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepoDetailUiState(
    val repo: Repository? = null,
    val commits: List<Commit> = emptyList(),
    val issues: List<Issue> = emptyList(),
    val pullRequests: List<PullRequest> = emptyList(),
    val branches: List<Branch> = emptyList(),
    val workflows: List<Workflow> = emptyList(),
    val workflowRuns: List<WorkflowRun> = emptyList(),
    val releases: List<Release> = emptyList(),
    val files: List<RepoContent> = emptyList(),
    val currentPath: String = "",
    val pathStack: List<String> = emptyList(),
    val readmeContent: String? = null,
    val fileContent: RepoContent? = null,
    val viewingFile: Boolean = false,
    val isLoadingFile: Boolean = false,
    val workflowJobs: List<WorkflowJob> = emptyList(),
    val viewingJobLogs: Long? = null,
    val jobLogs: String? = null,
    val isLoadingLogs: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreCommits: Boolean = true,
    val hasMoreIssues: Boolean = true,
    val hasMorePrs: Boolean = true,
    val hasMoreReleases: Boolean = true,
    val error: String? = null,
    val currentTab: Int = 0,
    val currentBranch: String? = null,
    val isStarred: Boolean = false,
    val isMonitoringActions: Boolean = false,
    val issueStateFilter: String = "open",
    val prStateFilter: String = "open",
    val issueComments: List<IssueComment> = emptyList(),
    val selectedIssueNumber: Int? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoDetailUiState())
    val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

    private var owner: String = ""
    private var repoName: String = ""
    private var initialized = false
    private var commitsPage = 1
    private var issuesPage = 1
    private var prsPage = 1
    private var releasesPage = 1
    private val perPage = 30

    fun init(owner: String, repo: String) {
        if (owner == this.owner && repoName == repo && initialized) return
        this.owner = owner
        this.repoName = repo
        initialized = true
        _uiState.value = RepoDetailUiState()
        loadRepo()
        checkStarred()
        loadCommits()
    }

    fun loadRepo() {
        viewModelScope.launch {
            gitHubRepository.getRepository(owner, repoName)
                .onSuccess { repo ->
                    _uiState.value = _uiState.value.copy(
                        repo = repo,
                        currentBranch = _uiState.value.currentBranch ?: repo.defaultBranch
                    )
                }
        }
    }

    fun checkStarred() {
        viewModelScope.launch {
            gitHubRepository.checkStarred(owner, repoName)
                .onSuccess { starred ->
                    _uiState.value = _uiState.value.copy(isStarred = starred)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isStarred = false)
                }
        }
    }

    fun toggleStar() {
        viewModelScope.launch {
            if (_uiState.value.isStarred) {
                gitHubRepository.unstarRepository(owner, repoName)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isStarred = false)
                        loadRepo()
                    }
            } else {
                gitHubRepository.starRepository(owner, repoName)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isStarred = true)
                        loadRepo()
                    }
            }
        }
    }

    fun loadCommits() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            commitsPage = 1
            val branch = _uiState.value.currentBranch
            gitHubRepository.getCommits(owner, repoName, branch = branch, page = commitsPage)
                .onSuccess { commits ->
                    _uiState.value = _uiState.value.copy(
                        commits = commits,
                        isLoading = false,
                        hasMoreCommits = commits.size >= perPage
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadMoreCommits() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreCommits) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            commitsPage++
            gitHubRepository.getCommits(owner, repoName, branch = _uiState.value.currentBranch, page = commitsPage)
                .onSuccess { commits ->
                    _uiState.value = _uiState.value.copy(
                        commits = _uiState.value.commits + commits,
                        isLoadingMore = false,
                        hasMoreCommits = commits.size >= perPage
                    )
                }
                .onFailure { e ->
                    commitsPage--
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
                }
        }
    }

    fun setIssueFilter(state: String) {
        _uiState.value = _uiState.value.copy(issueStateFilter = state)
        loadIssues()
    }

    fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            issuesPage = 1
            gitHubRepository.getIssues(owner, repoName, state = _uiState.value.issueStateFilter, page = issuesPage)
                .onSuccess { issues ->
                    val realIssues = issues.filter { it.pullRequest == null }
                    _uiState.value = _uiState.value.copy(
                        issues = realIssues,
                        isLoading = false,
                        hasMoreIssues = issues.size >= perPage
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadMoreIssues() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreIssues) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            issuesPage++
            gitHubRepository.getIssues(owner, repoName, state = _uiState.value.issueStateFilter, page = issuesPage)
                .onSuccess { issues ->
                    val realIssues = issues.filter { it.pullRequest == null }
                    _uiState.value = _uiState.value.copy(
                        issues = _uiState.value.issues + realIssues,
                        isLoadingMore = false,
                        hasMoreIssues = issues.size >= perPage
                    )
                }
                .onFailure { e ->
                    issuesPage--
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
                }
        }
    }

    fun setPrFilter(state: String) {
        _uiState.value = _uiState.value.copy(prStateFilter = state)
        loadPullRequests()
    }

    fun loadPullRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            prsPage = 1
            gitHubRepository.getPullRequests(owner, repoName, state = _uiState.value.prStateFilter, page = prsPage)
                .onSuccess { prs ->
                    _uiState.value = _uiState.value.copy(
                        pullRequests = prs,
                        isLoading = false,
                        hasMorePrs = prs.size >= perPage
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadMorePullRequests() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePrs) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            prsPage++
            gitHubRepository.getPullRequests(owner, repoName, state = _uiState.value.prStateFilter, page = prsPage)
                .onSuccess { prs ->
                    _uiState.value = _uiState.value.copy(
                        pullRequests = _uiState.value.pullRequests + prs,
                        isLoadingMore = false,
                        hasMorePrs = prs.size >= perPage
                    )
                }
                .onFailure { e ->
                    prsPage--
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
                }
        }
    }

    fun closeIssue(number: Int) {
        viewModelScope.launch {
            gitHubRepository.updateIssue(owner, repoName, number, state = "closed")
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionMessage = "Issue #${number} closed")
                    loadIssues()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun reopenIssue(number: Int) {
        viewModelScope.launch {
            gitHubRepository.updateIssue(owner, repoName, number, state = "open")
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionMessage = "Issue #${number} reopened")
                    loadIssues()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun mergePullRequest(number: Int) {
        viewModelScope.launch {
            gitHubRepository.mergePullRequest(owner, repoName, number)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionMessage = "PR #${number} merged")
                    loadPullRequests()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun loadIssueComments(number: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedIssueNumber = number)
            gitHubRepository.getIssueComments(owner, repoName, number)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(issueComments = comments)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(issueComments = emptyList())
                }
        }
    }

    fun addComment(body: String) {
        val number = _uiState.value.selectedIssueNumber ?: return
        viewModelScope.launch {
            gitHubRepository.createIssueComment(owner, repoName, number, body)
                .onSuccess {
                    loadIssueComments(number)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun loadFiles(path: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getRepoContent(owner, repoName, path, ref = _uiState.value.currentBranch)
                .onSuccess { files ->
                    val sorted = files.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
                    val newPathStack = if (path.isEmpty()) emptyList() else _uiState.value.pathStack + _uiState.value.currentPath
                    _uiState.value = _uiState.value.copy(
                        files = sorted,
                        currentPath = path,
                        pathStack = newPathStack,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun navigateUp() {
        val stack = _uiState.value.pathStack
        if (stack.isEmpty()) {
            _uiState.value = _uiState.value.copy(currentPath = "", pathStack = emptyList())
        } else {
            val parentPath = stack.last()
            _uiState.value = _uiState.value.copy(
                currentPath = parentPath,
                pathStack = stack.dropLast(1)
            )
        }
        loadFiles(_uiState.value.currentPath)
    }

    fun loadReadme() {
        viewModelScope.launch {
            gitHubRepository.getReadme(owner, repoName, ref = _uiState.value.currentBranch)
                .onSuccess { content ->
                    val decoded = content.content?.let { raw ->
                        try {
                            android.util.Base64.decode(raw.replace("\n", ""), android.util.Base64.DEFAULT).decodeToString()
                        } catch (e: Exception) { null }
                    }
                    _uiState.value = _uiState.value.copy(readmeContent = decoded)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(readmeContent = null)
                }
        }
    }

    fun loadBranches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getBranches(owner, repoName)
                .onSuccess { branches ->
                    _uiState.value = _uiState.value.copy(branches = branches, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun switchBranch(branch: String) {
        _uiState.value = _uiState.value.copy(currentBranch = branch)
        loadCommits()
    }

    fun loadWorkflows() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getWorkflows(owner, repoName)
                .onSuccess { workflows ->
                    _uiState.value = _uiState.value.copy(workflows = workflows, isLoading = false)
                    if (workflows.isNotEmpty()) {
                        loadWorkflowRuns()
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadWorkflowRuns() {
        viewModelScope.launch {
            gitHubRepository.getWorkflowRuns(owner, repoName)
                .onSuccess { response ->
val hasActive = response.workflowRuns?.any { it.status == "in_progress" || it.status == "queued" || it.status == "waiting" } ?: false
            _uiState.value = _uiState.value.copy(
                workflowRuns = response.workflowRuns ?: emptyList(),
                        isMonitoringActions = hasActive
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun dispatchWorkflow(workflowId: Long, ref: String) {
        viewModelScope.launch {
            gitHubRepository.dispatchWorkflow(owner, repoName, workflowId, ref)
            delay(2000)
            loadWorkflowRuns()
        }
    }

    fun reRunWorkflow(runId: Long) {
        viewModelScope.launch {
            gitHubRepository.reRunWorkflow(owner, repoName, runId)
            delay(2000)
            loadWorkflowRuns()
        }
    }

    fun cancelWorkflowRun(runId: Long) {
        viewModelScope.launch {
            gitHubRepository.cancelWorkflowRun(owner, repoName, runId)
            delay(1000)
            loadWorkflowRuns()
        }
    }

    fun loadReleases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            releasesPage = 1
            gitHubRepository.getReleases(owner, repoName)
                .onSuccess { releases ->
                    _uiState.value = _uiState.value.copy(
                        releases = releases,
                        isLoading = false,
                        hasMoreReleases = releases.size >= 20
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            val branch = _uiState.value.currentBranch
            gitHubRepository.getRepository(owner, repoName)
                .onSuccess { repo ->
                    _uiState.value = _uiState.value.copy(repo = repo)
                }
            when (_uiState.value.currentTab) {
                0 -> gitHubRepository.getCommits(owner, repoName, branch = branch, page = 1)
                    .onSuccess { _uiState.value = _uiState.value.copy(commits = it) }
                1 -> gitHubRepository.getIssues(owner, repoName, state = _uiState.value.issueStateFilter, page = 1)
                    .onSuccess { _uiState.value = _uiState.value.copy(issues = it.filter { i -> i.pullRequest == null }) }
                2 -> gitHubRepository.getPullRequests(owner, repoName, state = _uiState.value.prStateFilter, page = 1)
                    .onSuccess { _uiState.value = _uiState.value.copy(pullRequests = it) }
                3 -> gitHubRepository.getBranches(owner, repoName)
                    .onSuccess { _uiState.value = _uiState.value.copy(branches = it) }
                5 -> {
                    gitHubRepository.getWorkflows(owner, repoName).onSuccess { _uiState.value = _uiState.value.copy(workflows = it) }
                    gitHubRepository.getWorkflowRuns(owner, repoName).onSuccess { _uiState.value = _uiState.value.copy(workflowRuns = it.workflowRuns ?: emptyList()) }
                }
                6 -> gitHubRepository.getReleases(owner, repoName)
                    .onSuccess { _uiState.value = _uiState.value.copy(releases = it) }
            }
            checkStarred()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun onTabChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
        when (index) {
            0 -> loadCommits()
            1 -> loadIssues()
            2 -> loadPullRequests()
            3 -> loadBranches()
            4 -> loadFiles()
            5 -> { loadWorkflows(); loadWorkflowRuns() }
            6 -> loadReleases()
        }
    }

    fun forkRepo() {
        viewModelScope.launch {
            gitHubRepository.forkRepository(owner, repoName)
        }
    }

    fun deleteRepo() {
        viewModelScope.launch {
            gitHubRepository.deleteRepository(owner, repoName)
        }
    }

    fun createIssue(title: String, body: String?) {
        viewModelScope.launch {
            gitHubRepository.createIssue(owner, repoName, title, body)
            loadIssues()
        }
    }

    fun loadFileContent(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFile = true, viewingFile = true)
            gitHubRepository.getFileContent(owner, repoName, path, ref = _uiState.value.currentBranch)
                .onSuccess { content ->
                    _uiState.value = _uiState.value.copy(fileContent = content, isLoadingFile = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoadingFile = false, viewingFile = false)
                }
        }
    }

    fun closeFileViewer() {
        _uiState.value = _uiState.value.copy(viewingFile = false, fileContent = null)
    }

    fun loadWorkflowRunJobs(runId: Long) {
        viewModelScope.launch {
            gitHubRepository.getWorkflowRunJobs(owner, repoName, runId)
                .onSuccess { jobs ->
                    _uiState.value = _uiState.value.copy(workflowJobs = jobs)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun loadJobLogs(jobId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLogs = true, viewingJobLogs = jobId, jobLogs = null)
            gitHubRepository.getJobLogs(owner, repoName, jobId)
                .onSuccess { logs ->
                    _uiState.value = _uiState.value.copy(jobLogs = logs, isLoadingLogs = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(jobLogs = null, isLoadingLogs = false, error = e.message)
                }
        }
    }

    fun closeJobLogs() {
        _uiState.value = _uiState.value.copy(viewingJobLogs = null, jobLogs = null)
    }
}
