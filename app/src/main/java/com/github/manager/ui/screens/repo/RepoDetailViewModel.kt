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
    val isLoading: Boolean = false,
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
            val branch = _uiState.value.currentBranch
            gitHubRepository.getCommits(owner, repoName, branch = branch)
                .onSuccess { commits ->
                    _uiState.value = _uiState.value.copy(commits = commits, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
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
            gitHubRepository.getIssues(owner, repoName, state = _uiState.value.issueStateFilter)
                .onSuccess { issues ->
                    val realIssues = issues.filter { it.pullRequest == null }
                    _uiState.value = _uiState.value.copy(issues = realIssues, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
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
            gitHubRepository.getPullRequests(owner, repoName, state = _uiState.value.prStateFilter)
                .onSuccess { prs ->
                    _uiState.value = _uiState.value.copy(pullRequests = prs, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
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
                    val hasActive = response.workflowRuns.any { it.status == "in_progress" || it.status == "queued" || it.status == "waiting" }
                    _uiState.value = _uiState.value.copy(
                        workflowRuns = response.workflowRuns,
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
            gitHubRepository.getReleases(owner, repoName)
                .onSuccess { releases ->
                    _uiState.value = _uiState.value.copy(releases = releases, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
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
}
