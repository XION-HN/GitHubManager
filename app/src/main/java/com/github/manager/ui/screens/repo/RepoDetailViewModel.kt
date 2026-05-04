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
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTab: Int = 0,
    val currentBranch: String? = null,
    val isStarred: Boolean = false,
    val isMonitoringActions: Boolean = false
)

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoDetailUiState())
    val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

    private var owner: String = ""
    private var repoName: String = ""

    fun init(owner: String, repo: String) {
        this.owner = owner
        this.repoName = repo
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

    fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getIssues(owner, repoName)
                .onSuccess { issues ->
                    val realIssues = issues.filter { it.pullRequest == null }
                    _uiState.value = _uiState.value.copy(issues = realIssues, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadPullRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getPullRequests(owner, repoName)
                .onSuccess { prs ->
                    _uiState.value = _uiState.value.copy(pullRequests = prs, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
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

    fun onTabChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
        when (index) {
            0 -> loadCommits()
            1 -> loadIssues()
            2 -> loadPullRequests()
            3 -> loadBranches()
            4 -> {
                loadWorkflows()
                loadWorkflowRuns()
            }
        }
    }

    fun starRepo() {
        viewModelScope.launch {
            gitHubRepository.starRepository(owner, repoName)
            loadRepo()
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
